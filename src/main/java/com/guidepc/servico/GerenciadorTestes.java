package com.guidepc.servico;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guidepc.modelo.*;
import com.guidepc.persistencia.RepositorioResultados;
import com.guidepc.web.websocket.MonitoramentoWs;

import java.util.concurrent.*;

/**
 * Gerencia execução assíncrona de testes de estresse e disco.
 * Executa os testes em thread separada e envia callbacks via WebSocket.
 */
public final class GerenciadorTestes {

    private static final GerenciadorTestes INSTANCIA = new GerenciadorTestes();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ServicoColetorHardware coletor;
    private final ServicoTesteEstresse servicoEstresse;
    private final ServicoTesteDisco servicoDisco;
    private final ServicoAlerta servicoAlerta;

    // Pool de threads com 2 threads — permite executar teste de estresse e disco em paralelo
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    // Futures das tarefas em execução; volatile garante visibilidade entre threads
    private volatile Future<?> tarefaEstresse;
    private volatile Future<?> tarefaDisco;

    private GerenciadorTestes() {
        this.coletor = ServicoColetorHardware.obterInstancia();
        this.servicoAlerta = ServicoMonitoramento.obterInstancia().obterServicoAlerta();
        this.servicoEstresse = new ServicoTesteEstresse(coletor, servicoAlerta);
        this.servicoDisco = new ServicoTesteDisco();
    }

    public static GerenciadorTestes obterInstancia() {
        return INSTANCIA;
    }

    public boolean estaEmExecucao() {
        return (tarefaEstresse != null && !tarefaEstresse.isDone())
            || (tarefaDisco != null && !tarefaDisco.isDone());
    }

    /**
     * Inicia teste de estresse de forma assíncrona em thread separada.
     * O teste executa em background e envia callbacks via WebSocket.
     * synchronized impede que dois testes sejam iniciados ao mesmo tempo.
     */
    public synchronized void iniciarEstresse(String nivelStr, int duracao) {
        if (estaEmExecucao()) {
            throw new IllegalStateException("Já existe um teste em execução");
        }

        NivelEstresse nivel;
        try {
            nivel = NivelEstresse.valueOf(nivelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            nivel = NivelEstresse.BAIXO;
        }

        final NivelEstresse nivelFinal = nivel;
        // Submete a tarefa de estresse ao pool de threads
        tarefaEstresse = executor.submit(() -> {
            try {
                // Notifica início do teste (0% de progresso)
                MonitoramentoWs.enviarProgresso(0);
                // Executa o teste, passando callbacks para enviar dados ao WebSocket
                ResultadoTesteEstresse resultado = servicoEstresse.executarTeste(
                    nivelFinal, duracao,
                    // Callback de amostra: enviar métricas em tempo real
                    amostra -> MonitoramentoWs.enviarAmostra(
                        amostra.cargaCpuPercentual(),
                        amostra.usoMemoriaPercentual(),
                        amostra.temperaturaCelsius(),
                        amostra.tempoRespostaMs()
                    ),
                    // Callback de progresso: enviar porcentagem concluída
                    percentual -> MonitoramentoWs.enviarProgresso(percentual),
                    // Callback de alertas: enviar alertas detectados
                    alertas -> {
                        for (Alerta alerta : alertas) {
                            MonitoramentoWs.enviarAlerta(alerta.toString());
                        }
                    }
                );

                // Salvar resultado no banco SQLite
                String hardwareJson = "{}";
                try {
                    ObjectNode hw = MAPPER.createObjectNode();
                    var proc = coletor.obterInformacoesProcessador();
                    hw.put("cpu", proc.fabricante() + " " + proc.modelo());
                    hw.put("nucleos", proc.nucleosLogicos());
                    var mem = coletor.obterInformacoesMemoria();
                    hw.put("ram", String.format("%.1f GB", mem.totalBytes() / (1024.0 * 1024 * 1024)));
                    hardwareJson = MAPPER.writeValueAsString(hw);
                } catch (Exception ignored) {}

                RepositorioResultados.salvarTesteEstresse(resultado, hardwareJson);

                // Monta e envia o resultado final via WebSocket para a interface
                ObjectNode resultadoNode = MAPPER.createObjectNode();
                resultadoNode.put("tipo", "RESULTADO");
                resultadoNode.put("tipoTeste", "ESTRESSE");
                resultadoNode.put("nivel", resultado.obterNivelEstresse().name());
                resultadoNode.put("duracao", resultado.obterDuracaoSegundos());
                resultadoNode.put("mediaCpu", resultado.obterMediaCpu());
                resultadoNode.put("maxCpu", resultado.obterMaximoCpu());
                resultadoNode.put("selo", resultado.obterSeloDesempenho());
                MonitoramentoWs.enviarResultado(MAPPER.writeValueAsString(resultadoNode));

            } catch (InterruptedException e) {
                // Restaura o status de interrupção da thread ao capturar InterruptedException
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                MonitoramentoWs.enviarAlerta("Erro no teste de estresse: " + e.getMessage());
            }
        });
    }

    /**
     * Inicia teste de disco de forma assíncrona em thread separada.
     * segue o mesmo padrão do teste de estresse.
     */
    public synchronized void iniciarDisco(String tipoStr, int duracao) {
        if (estaEmExecucao()) {
            throw new IllegalStateException("Já existe um teste em execução");
        }

        ResultadoTesteDisco.TipoTesteDisco tipo;
        try {
            tipo = ResultadoTesteDisco.TipoTesteDisco.valueOf(tipoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            tipo = ResultadoTesteDisco.TipoTesteDisco.ESCRITA_SEQUENCIAL;
        }

        final ResultadoTesteDisco.TipoTesteDisco tipoFinal = tipo;
        // Submete a tarefa de disco ao pool de threads
        tarefaDisco = executor.submit(() -> {
            try {
                MonitoramentoWs.enviarProgresso(0);
                // Executa o teste de disco
                ResultadoTesteDisco resultado = servicoDisco.executarTeste(
                    tipoFinal, duracao,
                    percentual -> MonitoramentoWs.enviarProgresso(percentual)
                );

                // Salvar resultado no banco SQLite
                String hardwareJson = "{}";
                try {
                    ObjectNode hw = MAPPER.createObjectNode();
                    var proc = coletor.obterInformacoesProcessador();
                    hw.put("cpu", proc.fabricante() + " " + proc.modelo());
                    hw.put("nucleos", proc.nucleosLogicos());
                    var mem = coletor.obterInformacoesMemoria();
                    hw.put("ram", String.format("%.1f GB", mem.totalBytes() / (1024.0 * 1024 * 1024)));
                    hardwareJson = MAPPER.writeValueAsString(hw);
                } catch (Exception ignored) {}

                RepositorioResultados.salvarTesteDisco(resultado, hardwareJson);

                // Monta e envia o resultado final via WebSocket
                ObjectNode resultadoNode = MAPPER.createObjectNode();
                resultadoNode.put("tipo", "RESULTADO");
                resultadoNode.put("tipoTeste", "DISCO");
                resultadoNode.put("tipoDisco", resultado.tipoTeste().name());
                resultadoNode.put("duracao", resultado.duracaoSegundos());
                resultadoNode.put("throughputMbps", resultado.obterThroughputMBs());
                resultadoNode.put("iops", resultado.iops());
                resultadoNode.put("selo", classificarThroughput(resultado.obterThroughputMBs()));
                MonitoramentoWs.enviarResultado(MAPPER.writeValueAsString(resultadoNode));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                MonitoramentoWs.enviarAlerta("Erro no teste de disco: " + e.getMessage());
            }
        });
    }

    /**
     * Para qualquer teste em execução.
     * Solicita parada graciosa e cancela a tarefa assíncrona.
     */
    public synchronized void parar() {
        if (tarefaEstresse != null && !tarefaEstresse.isDone()) {
            servicoEstresse.solicitarParada();
            tarefaEstresse.cancel(true);
        }
        if (tarefaDisco != null && !tarefaDisco.isDone()) {
            servicoDisco.solicitarParada();
            tarefaDisco.cancel(true);
        }
    }

    /** Classifica o throughput do disco em categorias compreensíveis. */
    private static String classificarThroughput(double mbPorSegundo) {
        if (mbPorSegundo >= 500) return "Excelente";
        if (mbPorSegundo >= 100) return "Bom";
        if (mbPorSegundo >= 50) return "Regular";
        return "Lento";
    }
}
