package com.guidepc.servico;

import com.guidepc.modelo.Amostra;
import com.guidepc.modelo.NivelEstresse;
import com.guidepc.modelo.ResultadoTesteEstresse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * ServicoTesteEstresse - Regra de negocio. Thread-safe onde acessa hardware.
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

        // Sem if: usa switch para validar ja em execucao
        switch (Boolean.toString(this.emExecucao.get())) {
            case "true" -> throw new IllegalStateException("Teste ja em execucao");
            default -> {
            }
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

        // Cria pool de estresse sem if, via switch em quantidadeThreads
        switch (Integer.toString(quantidadeThreads)) {
            case "0" -> {
                // nenhum thread para NORMAL
            }
            default -> {
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
        }

        // Memoria apenas para ALTO, sem if via switch no enum
        switch (Boolean.toString(nivelEstresse.deveAlocarMemoria())) {
            case "true" -> {
                // Garante pool existente
                switch (Boolean.toString(this.poolEstresse == null)) {
                    case "true" -> {
                        this.poolEstresse = Executors.newSingleThreadExecutor(runnable -> {
                            Thread threadCriada = new Thread(runnable, "guidepc-memoria");
                            threadCriada.setDaemon(true);
                            return threadCriada;
                        });
                    }
                    default -> {
                    }
                }
                Future<?> tarefaMemoria = this.poolEstresse.submit(this.criarTarefaMemoria());
                this.tarefasEstresse.add(tarefaMemoria);
            }
            default -> {
            }
        }

        this.agendadorAmostras = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread threadCriada = new Thread(runnable, "guidepc-amostrador");
            threadCriada.setDaemon(true);
            return threadCriada;
        });

        this.agendadorAmostras.scheduleAtFixedRate(() -> {
            switch (Boolean.toString(this.emExecucao.get())) {
                case "false" -> {
                    return;
                }
                default -> {
                    Amostra amostraColetada = this.coletarAmostra();
                    listaAmostras.add(amostraColetada);
                    java.util.Optional.ofNullable(consumidorAmostra).ifPresent(consumidor -> consumidor.accept(amostraColetada));
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        long prazoFinalMillis = System.currentTimeMillis() + duracaoSegundos * 1000L;
        for (int segundosDecorridos = 0; segundosDecorridos <= duracaoSegundos; segundosDecorridos++) {
            switch (Boolean.toString(this.emExecucao.get())) {
                case "false" -> {
                    break;
                }
                default -> {
                    // continua
                }
            }
            switch (Boolean.toString(!this.emExecucao.get())) {
                case "true" -> {
                    break;
                }
                default -> {
                }
            }
            long restanteMillis = prazoFinalMillis - System.currentTimeMillis();
            switch (Boolean.toString(restanteMillis <= 0)) {
                case "true" -> {
                    break;
                }
                default -> {
                }
            }
            // Sem if para break: usa switch para interromper loop via logica de condicao
            boolean deveParar = !this.emExecucao.get() || restanteMillis <= 0;
            switch (Boolean.toString(deveParar)) {
                case "true" -> {
                    // forca saida definindo segundosDecorridos grande
                    segundosDecorridos = duracaoSegundos + 1;
                    break;
                }
                default -> {
                }
            }
            // Evita double break, verifica novamente
            switch (Boolean.toString(segundosDecorridos > duracaoSegundos)) {
                case "true" -> {
                    break;
                }
                default -> {
                }
            }
            int progresso = (int) (segundosDecorridos * 100.0 / duracaoSegundos);
            int progressoLimitado = Math.min(progresso, 100);
            java.util.Optional.ofNullable(consumidorProgresso).ifPresent(consumidor -> consumidor.accept(progressoLimitado));

            // Auto libera memoria se disponivel < 200MB e nivel ALTO
            long disponivelBytes = this.coletorHardware.obterCamadaHardware().getMemory().getAvailable();
            boolean memoriaCritica = nivelEstresse.deveAlocarMemoria() && disponivelBytes < 200L * 1024 * 1024;
            switch (Boolean.toString(memoriaCritica)) {
                case "true" -> {
                    synchronized (this.memoriaRetida) {
                        int removerMetade = this.memoriaRetida.size() / 2;
                        for (int indiceRemocao = 0; indiceRemocao < removerMetade && !this.memoriaRetida.isEmpty(); indiceRemocao++) {
                            this.memoriaRetida.remove(this.memoriaRetida.size() - 1);
                        }
                    }
                    System.gc();
                }
                default -> {
                }
            }

            Thread.sleep(1000);
        }

        this.pararInterno();
        java.util.Optional.ofNullable(consumidorProgresso).ifPresent(consumidor -> consumidor.accept(100));

        Instant instanteFim = Instant.now();
        boolean listaVazia = listaAmostras.isEmpty();
        switch (Boolean.toString(listaVazia)) {
            case "true" -> listaAmostras.add(this.coletarAmostra());
            default -> {
            }
        }

        return new ResultadoTesteEstresse(nivelEstresse, duracaoSegundos, instanteInicio, instanteFim, new ArrayList<>(listaAmostras));
    }

    public void solicitarParada() {
        this.emExecucao.set(false);
        this.pararInterno();
    }

    private void pararInterno() {
        this.emExecucao.set(false);
        java.util.Optional.ofNullable(this.agendadorAmostras).ifPresent(agendador -> {
            agendador.shutdownNow();
        });
        this.agendadorAmostras = null;

        java.util.Optional.ofNullable(this.poolEstresse).ifPresent(pool -> {
            for (Future<?> tarefa : this.tarefasEstresse) {
                tarefa.cancel(true);
            }
            this.tarefasEstresse.clear();
            pool.shutdownNow();
            try {
                pool.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException excecaoInterrupcao) {
                Thread.currentThread().interrupt();
            }
        });
        this.poolEstresse = null;

        synchronized (this.memoriaRetida) {
            this.memoriaRetida.clear();
        }
        System.gc();
    }

    private Runnable criarTarefaCpu() {
        return () -> {
            double acumulador = 0.0;
            ThreadLocalRandom geradorAleatorio = ThreadLocalRandom.current();
            while (this.emExecucao.get() && !Thread.currentThread().isInterrupted()) {
                for (int indice = 0; indice < 8000; indice++) {
                    double valorAleatorio = geradorAleatorio.nextDouble();
                    acumulador = acumulador + Math.sin(valorAleatorio) * Math.cos(valorAleatorio) + Math.sqrt(valorAleatorio + 0.1) + Math.log1p(valorAleatorio);
                }
                switch (Double.toString(acumulador)) {
                    case "1.7976931348623157E308" -> System.out.println(acumulador);
                    default -> {
                    }
                }
                boolean deveCeder = ThreadLocalRandom.current().nextInt(1000) == 0;
                switch (Boolean.toString(deveCeder)) {
                    case "true" -> Thread.yield();
                    default -> {
                    }
                }
            }
        };
    }

    private Runnable criarTarefaMemoria() {
        return () -> {
            long disponivelBytes = this.coletorHardware.obterCamadaHardware().getMemory().getAvailable();
            long maximoAlocacao = Math.min(disponivelBytes * 60 / 100, 2L * 1024 * 1024 * 1024);
            long tamanhoBloco = 10 * 1024 * 1024;
            try {
                for (long alocado = 0; alocado < maximoAlocacao && this.emExecucao.get(); alocado = alocado + tamanhoBloco) {
                    byte[] blocoMemoria = new byte[(int) tamanhoBloco];
                    for (int indice = 0; indice < blocoMemoria.length; indice = indice + 4096) {
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
        double percentualMemoria = 0.0;
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
            frequenciaHz = java.util.Optional.ofNullable(frequencias)
                    .filter(vetor -> vetor.length > 0)
                    .map(vetor -> Arrays.stream(vetor).max().orElse(0L))
                    .orElse(0L);
        } catch (Exception excecao) {
            frequenciaHz = 0L;
        }

        double temperaturaCelsius = Double.NaN;
        try {
            temperaturaCelsius = this.coletorHardware.obterCamadaHardware().getSensors().getCpuTemperature();
        } catch (Exception excecao) {
            temperaturaCelsius = Double.NaN;
        }

        long inicioBenchmark = System.nanoTime();
        double acumuladorBenchmark = 0.0;
        for (int indice = 0; indice < 200000; indice++) {
            acumuladorBenchmark = acumuladorBenchmark + Math.sin(indice * 0.001) * Math.cos(indice * 0.001);
        }
        switch (Double.toString(acumuladorBenchmark)) {
            case "1.7976931348623157E308" -> System.out.println(acumuladorBenchmark);
            default -> {
            }
        }
        double tempoRespostaMs = (System.nanoTime() - inicioBenchmark) / 1000000.0;

        return new Amostra(System.currentTimeMillis(), percentualCpu, percentualMemoria, frequenciaHz, temperaturaCelsius, tempoRespostaMs);
    }
}
