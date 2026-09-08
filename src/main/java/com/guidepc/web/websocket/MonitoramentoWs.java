package com.guidepc.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guidepc.servico.ServicoColetorHardware;
import com.guidepc.utilitario.VersaoApp;
import io.javalin.websocket.WsContext;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket de monitoramento — envia métricas em tempo real a cada 1s.
 */
public final class MonitoramentoWs {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<WsContext> CLIENTES = new CopyOnWriteArraySet<>();

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "GuidePC-WS-Monitor");
        t.setDaemon(true);
        return t;
    });

    private static volatile ScheduledFuture<?> tarefaAtiva = null;

    private MonitoramentoWs() {
    }

    public static void onConnect(WsContext ctx) {
        CLIENTES.add(ctx);
        synchronized (MonitoramentoWs.class) {
            if (tarefaAtiva == null || tarefaAtiva.isCancelled()) {
                tarefaAtiva = SCHEDULER.scheduleAtFixedRate(MonitoramentoWs::enviarMetricas, 1, 1, TimeUnit.SECONDS);
            }
        }
    }

    public static void onClose(WsContext ctx) {
        CLIENTES.remove(ctx);
        if (CLIENTES.isEmpty()) {
            synchronized (MonitoramentoWs.class) {
                if (tarefaAtiva != null) {
                    tarefaAtiva.cancel(false);
                    tarefaAtiva = null;
                }
            }
        }
    }

    public static void onMessage(WsContext ctx, String mensagem) {
    }

    public static void enviarMetricas() {
        if (CLIENTES.isEmpty()) return;

        try {
            ServicoColetorHardware coletor = ServicoColetorHardware.obterInstancia();
            var processador = coletor.obterInformacoesProcessador();
            var memoria = coletor.obterInformacoesMemoria();
            var usoGpu = coletor.obterUsoGpu();
            var tempGpu = coletor.obterTemperaturaGpu();
            var bateria = coletor.obterBateria();

            ObjectNode root = MAPPER.createObjectNode();
            root.put("tipo", "METRICAS");

            ObjectNode cpuNode = MAPPER.createObjectNode();
            cpuNode.put("usoPercentual", processador.percentualUso());
            cpuNode.put("temperaturaCelsius", processador.temperaturaCelsius());
            cpuNode.put("frequenciaMaximaHz", processador.frequenciaMaximaHz());
            cpuNode.put("nucleosFisicos", processador.nucleosFisicos());
            cpuNode.put("nucleosLogicos", processador.nucleosLogicos());
            root.set("cpu", cpuNode);

            ObjectNode ramNode = MAPPER.createObjectNode();
            ramNode.put("totalBytes", memoria.totalBytes());
            ramNode.put("disponivelBytes", memoria.disponivelBytes());
            ramNode.put("emUsoBytes", memoria.emUsoBytes());
            ramNode.put("percentualUso", memoria.percentualUso());
            root.set("memoria", ramNode);

            ObjectNode gpuNode = MAPPER.createObjectNode();
            gpuNode.put("usoPercentual", usoGpu);
            gpuNode.put("temperaturaCelsius", tempGpu);
            root.set("gpu", gpuNode);

            if (bateria != null) {
                ObjectNode batNode = MAPPER.createObjectNode();
                batNode.put("percentual", bateria.percentual());
                batNode.put("carregando", bateria.carregando());
                batNode.put("tempoRestanteSegundos", bateria.tempoRestanteSegundos());
                root.set("bateria", batNode);
            }

            String json = MAPPER.writeValueAsString(root);

            for (WsContext cliente : CLIENTES) {
                try {
                    cliente.send(json);
                } catch (Exception e) {
                    CLIENTES.remove(cliente);
                }
            }
        } catch (Exception e) {
            // Ignora erros momentaneos de coleta
        }
    }

    public static void enviarProgresso(double percentual) {
        enviarParaTodos(tipo("PROGRESSO").put("percentual", percentual));
    }

    public static void enviarAmostra(double cpu, double memoriaUso, double temp, double resposta) {
        ObjectNode node = tipo("AMOSTRA");
        node.put("cargaCpu", cpu);
        node.put("usoMemoria", memoriaUso);
        node.put("temperatura", temp);
        node.put("tempoRespostaMs", resposta);
        enviarParaTodos(node);
    }

    public static void enviarResultado(String jsonResultado) {
        try {
            ObjectNode node = MAPPER.readValue(jsonResultado, ObjectNode.class);
            node.put("tipo", "RESULTADO");
            enviarParaTodos(node);
        } catch (IOException e) {
            ObjectNode node = tipo("RESULTADO");
            node.put("raw", jsonResultado);
            enviarParaTodos(node);
        }
    }

    public static void enviarAlerta(String mensagem) {
        enviarParaTodos(tipo("ALERTA").put("mensagem", mensagem));
    }

    private static void enviarParaTodos(ObjectNode node) {
        try {
            String json = MAPPER.writeValueAsString(node);
            for (WsContext cliente : CLIENTES) {
                try {
                    cliente.send(json);
                } catch (Exception e) {
                    CLIENTES.remove(cliente);
                }
            }
        } catch (Exception e) {
        }
    }

    private static ObjectNode tipo(String tipo) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("tipo", tipo);
        return node;
    }
}
