package com.guidepc.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guidepc.modelo.InformacoesProcessador;
import com.guidepc.servico.ServicoColetorHardware;
import io.javalin.websocket.WsContext;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket de monitoramento — envia métricas em tempo real a cada 1s.
 */
public final class MonitoramentoWs {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Conjunto thread-safe de clientes WebSocket conectados
    // CopyOnWriteArraySet permite iteração segura enquanto novos clientes conectam
    private static final Set<WsContext> CLIENTES = new CopyOnWriteArraySet<>();

    // Agendador que executa o envio de métricas a cada 1 segundo
    // Thread daemon para não impedir o encerramento da JVM
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "GuidePC-WS-Monitor");
        t.setDaemon(true);
        return t;
    });

    // Flag que controla se o agendamento de métricas está ativo
    private static volatile boolean monitoramentoAtivo = false;

    private MonitoramentoWs() {
    }

    /**
     * Chamado quando um cliente WebSocket se conecta.
     * Adiciona o cliente à lista e inicia o envio de métricas se ainda não estiver ativo.
     */
    public static void onConnect(WsContext ctx) {
        CLIENTES.add(ctx);
        // Inicia o agendador apenas na primeira conexão (lazy start)
        if (!monitoramentoAtivo) {
            monitoramentoAtivo = true;
            // Envia métricas a cada 1 segundo (delay inicial 1s, período 1s)
            SCHEDULER.scheduleAtFixedRate(MonitoramentoWs::enviarMetricas, 1, 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Chamado quando um cliente WebSocket se desconecta.
     * Remove o cliente da lista e para o agendador se não houver mais clientes.
     */
    public static void onClose(WsContext ctx) {
        CLIENTES.remove(ctx);
        // Para o agendador quando não há mais clientes (economiza recursos)
        if (CLIENTES.isEmpty()) {
            monitoramentoAtivo = false;
        }
    }

    public static void onMessage(WsContext ctx, String mensagem) {
        // Mensagens recebidas dos clientes (reservado para uso futuro)
    }

    /**
     * Coleta métricas de hardware e envia para todos os clientes conectados.
     * Chamada automaticamente a cada 1 segundo pelo agendador.
     */
    public static void enviarMetricas() {
        if (CLIENTES.isEmpty()) return;

        try {
            ServicoColetorHardware coletor = ServicoColetorHardware.obterInstancia();
            var processador = coletor.obterInformacoesProcessador();
            var memoria = coletor.obterInformacoesMemoria();
            var usoGpu = coletor.obterUsoGpu();
            var tempGpu = coletor.obterTemperaturaGpu();

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

            String json = MAPPER.writeValueAsString(root);

            // Envia o JSON para todos os clientes conectados
            synchronized (CLIENTES) {
                for (WsContext cliente : CLIENTES) {
                    try {
                        cliente.send(json);
                    } catch (Exception e) {
                        // Remove cliente com erro de envio
                        CLIENTES.remove(cliente);
                    }
                }
            }
        } catch (Exception e) {
            // Ignora erros momentâneos de coleta
        }
    }

    /**
     * Envia progresso do teste em execução.
     */
    public static void enviarProgresso(double percentual) {
        enviarParaTodos(tipo("PROGRESSO").put("percentual", percentual));
    }

    /**
     * Envia amostra de dados do teste.
     */
    public static void enviarAmostra(double cpu, double memoriaUso, double temp, double resposta) {
        ObjectNode node = tipo("AMOSTRA");
        node.put("cargaCpu", cpu);
        node.put("usoMemoria", memoriaUso);
        node.put("temperatura", temp);
        node.put("tempoRespostaMs", resposta);
        enviarParaTodos(node);
    }

    /**
     * Envia resultado final do teste.
     */
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

    /**
     * Envia alerta para todos os clientes.
     */
    public static void enviarAlerta(String mensagem) {
        enviarParaTodos(tipo("ALERTA").put("mensagem", mensagem));
    }

    /** Serializa o nó JSON e envia para todos os clientes conectados. */
    private static void enviarParaTodos(ObjectNode node) {
        try {
            String json = MAPPER.writeValueAsString(node);
            synchronized (CLIENTES) {
                for (WsContext cliente : CLIENTES) {
                    try {
                        cliente.send(json);
                    } catch (Exception e) {
                        CLIENTES.remove(cliente);
                    }
                }
            }
        } catch (Exception e) {
            // Ignora
        }
    }

    /** Cria um ObjectNode com o campo "tipo" preenchido (usado para mensagens padronizadas). */
    private static ObjectNode tipo(String tipo) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("tipo", tipo);
        return node;
    }
}
