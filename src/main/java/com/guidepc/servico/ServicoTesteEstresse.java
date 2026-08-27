package com.guidepc.servico;

import com.guidepc.modelo.Amostra;
import com.guidepc.modelo.NivelEstresse;
import com.guidepc.modelo.ResultadoTesteEstresse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Executa o teste de estresse e coleta amostras periodicas.
 *
 * <p>Fluxo:</p>
 * <ol>
 *   <li>cria pool de CPU conforme o nivel (0 para NORMAL, N/2 para BAIXO, N para ALTO);</li>
 *   <li>se ALTO, inicia tambem tarefa que aloca memoria em blocos de 10 MB ate 60% da RAM livre (teto 2 GB);</li>
 *   <li>agenda coleta de amostras a cada 500 ms;</li>
 *   <li>atualiza progresso segundo a segundo e libera metade da memoria se cair abaixo de 200 MB livres.</li>
 * </ol>
 *
 * <p>Threads sao daemon para nao impedir o encerramento da JVM. A parada e cooperativa
 * via {@link AtomicBoolean} e cancelamento de futures.</p>
 */
public class ServicoTesteEstresse {

    private final ServicoColetorHardware coletorHardware;
    private ExecutorService poolEstresse;
    private ScheduledExecutorService agendadorAmostras;
    private final AtomicBoolean emExecucao;
    private final List<byte[]> memoriaRetida;
    private final List<Future<?>> tarefasEstresse;

    public ServicoTesteEstresse() {
        this.coletorHardware = ServicoColetorHardware.obterInstancia();
        this.emExecucao = new AtomicBoolean(false);
        this.memoriaRetida = Collections.synchronizedList(new ArrayList<>());
        this.tarefasEstresse = new ArrayList<>();
    }

    public boolean estaEmExecucao() {
        return this.emExecucao.get();
    }

    public ResultadoTesteEstresse executarTeste(
            NivelEstresse nivelEstresse,
            int duracaoSegundos,
            Consumer<Amostra> consumidorAmostra,
            Consumer<Integer> consumidorProgresso
    ) throws InterruptedException {

        if (this.emExecucao.get()) {
            throw new IllegalStateException("Teste ja em execucao");
        }

        this.emExecucao.set(true);
        this.memoriaRetida.clear();
        this.tarefasEstresse.clear();

        Instant instanteInicio = Instant.now();
        List<Amostra> listaAmostras = Collections.synchronizedList(new ArrayList<>());

        int nucleosLogicos = Runtime.getRuntime().availableProcessors();
        int quantidadeThreads = switch (nivelEstresse) {
            case NORMAL -> 0;
            case BAIXO -> Math.max(1, nucleosLogicos / 2);
            case ALTO -> nucleosLogicos;
        };

        // Pool de CPU: apenas se o nivel exige carga
        if (quantidadeThreads > 0) {
            this.poolEstresse = Executors.newFixedThreadPool(quantidadeThreads, runnable -> {
                Thread threadCriada = new Thread(runnable, "guidepc-estresse");
                threadCriada.setDaemon(true);
                return threadCriada;
            });
            for (int indiceThread = 0; indiceThread < quantidadeThreads; indiceThread++) {
                Future<?> tarefa = this.poolEstresse.submit(this.criarTarefaCpu());
                this.tarefasEstresse.add(tarefa);
            }
        }

        // Pressao de memoria exclusiva do nivel ALTO
        if (nivelEstresse.deveAlocarMemoria()) {
            if (this.poolEstresse == null) {
                this.poolEstresse = Executors.newSingleThreadExecutor(runnable -> {
                    Thread threadCriada = new Thread(runnable, "guidepc-memoria");
                    threadCriada.setDaemon(true);
                    return threadCriada;
                });
            }
            Future<?> tarefaMemoria = this.poolEstresse.submit(this.criarTarefaMemoria());
            this.tarefasEstresse.add(tarefaMemoria);
        }

        this.agendadorAmostras = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread threadCriada = new Thread(runnable, "guidepc-amostrador");
            threadCriada.setDaemon(true);
            return threadCriada;
        });

        this.agendadorAmostras.scheduleAtFixedRate(() -> {
            if (!this.emExecucao.get()) {
                return;
            }
            Amostra amostraColetada = this.coletarAmostra();
            listaAmostras.add(amostraColetada);
            if (consumidorAmostra != null) {
                consumidorAmostra.accept(amostraColetada);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        long prazoFinalMillis = System.currentTimeMillis() + duracaoSegundos * 1000L;
        for (int segundosDecorridos = 0; segundosDecorridos <= duracaoSegundos; segundosDecorridos++) {
            if (!this.emExecucao.get()) {
                break;
            }
            long restanteMillis = prazoFinalMillis - System.currentTimeMillis();
            if (restanteMillis <= 0) {
                break;
            }

            int progresso = (int) (segundosDecorridos * 100.0 / duracaoSegundos);
            int progressoLimitado = Math.min(progresso, 100);
            if (consumidorProgresso != null) {
                consumidorProgresso.accept(progressoLimitado);
            }

            // Protecao: se a RAM livre cair abaixo de 200 MB no ALTO, libera metade e sugere GC
            long disponivelBytes = this.coletorHardware.obterCamadaHardware().getMemory().getAvailable();
            boolean memoriaCritica = nivelEstresse.deveAlocarMemoria() && disponivelBytes < 200L * 1024 * 1024;
            if (memoriaCritica) {
                synchronized (this.memoriaRetida) {
                    int removerMetade = this.memoriaRetida.size() / 2;
                    for (int indiceRemocao = 0; indiceRemocao < removerMetade && !this.memoriaRetida.isEmpty(); indiceRemocao++) {
                        this.memoriaRetida.remove(this.memoriaRetida.size() - 1);
                    }
                }
                System.gc();
            }

            Thread.sleep(1000);
        }

        this.pararInterno();
        if (consumidorProgresso != null) {
            consumidorProgresso.accept(100);
        }

        Instant instanteFim = Instant.now();
        if (listaAmostras.isEmpty()) {
            listaAmostras.add(this.coletarAmostra());
        }

        return new ResultadoTesteEstresse(nivelEstresse, duracaoSegundos, instanteInicio, instanteFim, new ArrayList<>(listaAmostras));
    }

    public void solicitarParada() {
        this.emExecucao.set(false);
        this.pararInterno();
    }

    private void pararInterno() {
        this.emExecucao.set(false);
        if (this.agendadorAmostras != null) {
            this.agendadorAmostras.shutdownNow();
            this.agendadorAmostras = null;
        }

        if (this.poolEstresse != null) {
            for (Future<?> tarefa : this.tarefasEstresse) {
                tarefa.cancel(true);
            }
            this.tarefasEstresse.clear();
            this.poolEstresse.shutdownNow();
            try {
                this.poolEstresse.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException excecaoInterrupcao) {
                Thread.currentThread().interrupt();
            }
            this.poolEstresse = null;
        }

        synchronized (this.memoriaRetida) {
            this.memoriaRetida.clear();
        }
        System.gc();
    }

    /**
     * Tarefa de CPU: loop com operacoes matematicas (sin/cos/sqrt/log) para manter
     * o nucleo ocupado. O acumulador evita otimizacao agressiva do JIT; yield
     * ocasional reduz starvation do agendador.
     */
    private Runnable criarTarefaCpu() {
        return () -> {
            double acumulador = 0.0;
            ThreadLocalRandom geradorAleatorio = ThreadLocalRandom.current();
            while (this.emExecucao.get() && !Thread.currentThread().isInterrupted()) {
                for (int indice = 0; indice < 8000; indice++) {
                    double valorAleatorio = geradorAleatorio.nextDouble();
                    acumulador += Math.sin(valorAleatorio) * Math.cos(valorAleatorio)
                            + Math.sqrt(valorAleatorio + 0.1)
                            + Math.log1p(valorAleatorio);
                }
                // Evita que o JIT elimine o loop por acumulador nao usado
                if (acumulador == Double.MAX_VALUE) {
                    System.out.println(acumulador);
                }
                if (ThreadLocalRandom.current().nextInt(1000) == 0) {
                    Thread.yield();
                }
            }
        };
    }

    /**
     * Tarefa de memoria: aloca blocos de 10 MB ate atingir 60% da RAM livre
     * no inicio do teste, com teto de 2 GB. Em OutOfMemory, libera tudo e
     * encerra o teste de forma segura.
     */
    private Runnable criarTarefaMemoria() {
        return () -> {
            long disponivelBytes = this.coletorHardware.obterCamadaHardware().getMemory().getAvailable();
            long maximoAlocacao = Math.min(disponivelBytes * 60 / 100, 2L * 1024 * 1024 * 1024);
            long tamanhoBloco = 10 * 1024 * 1024;
            try {
                for (long alocado = 0; alocado < maximoAlocacao && this.emExecucao.get(); alocado += tamanhoBloco) {
                    byte[] blocoMemoria = new byte[(int) tamanhoBloco];
                    // Toca uma pagina a cada 4 KB para garantir commit fisico
                    for (int indice = 0; indice < blocoMemoria.length; indice += 4096) {
                        blocoMemoria[indice] = 1;
                    }
                    this.memoriaRetida.add(blocoMemoria);
                    Thread.sleep(80);
                }
            } catch (OutOfMemoryError erroMemoria) {
                synchronized (this.memoriaRetida) {
                    this.memoriaRetida.clear();
                }
                System.gc();
                this.emExecucao.set(false);
            } catch (InterruptedException excecaoInterrupcao) {
                Thread.currentThread().interrupt();
            }
        };
    }

    private Amostra coletarAmostra() {
        double percentualCpu = this.coletorHardware.obterPercentualCargaCpu();
        double percentualMemoria;
        try {
            long totalBytes = this.coletorHardware.obterCamadaHardware().getMemory().getTotal();
            long disponivelBytes = this.coletorHardware.obterCamadaHardware().getMemory().getAvailable();
            percentualMemoria = totalBytes > 0 ? (totalBytes - disponivelBytes) * 100.0 / totalBytes : 0.0;
        } catch (Exception excecao) {
            percentualMemoria = Double.NaN;
        }

        long frequenciaHz = 0L;
        try {
            long[] frequencias = this.coletorHardware.obterCamadaHardware().getProcessor().getCurrentFreq();
            if (frequencias != null && frequencias.length > 0) {
                frequenciaHz = Arrays.stream(frequencias).max().orElse(0L);
            }
        } catch (Exception excecao) {
            frequenciaHz = 0L;
        }

        double temperaturaCelsius = Double.NaN;
        try {
            temperaturaCelsius = this.coletorHardware.obterCamadaHardware().getSensors().getCpuTemperature();
        } catch (Exception excecao) {
            temperaturaCelsius = Double.NaN;
        }

        // Micro-benchmark de responsividade: mede tempo de 200k iteracoes sin/cos
        long inicioBenchmark = System.nanoTime();
        double acumuladorBenchmark = 0.0;
        for (int indice = 0; indice < 200000; indice++) {
            acumuladorBenchmark += Math.sin(indice * 0.001) * Math.cos(indice * 0.001);
        }
        if (acumuladorBenchmark == Double.MAX_VALUE) {
            System.out.println(acumuladorBenchmark);
        }
        double tempoRespostaMs = (System.nanoTime() - inicioBenchmark) / 1000000.0;

        return new Amostra(System.currentTimeMillis(), percentualCpu, percentualMemoria, frequenciaHz, temperaturaCelsius, tempoRespostaMs);
    }
}
