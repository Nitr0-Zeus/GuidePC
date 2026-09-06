package com.guidepc.servico;

import com.guidepc.modelo.ResultadoTesteDisco;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Servico de teste de disco - leitura e escrita sequencial/aleatoria.
 */
public class ServicoTesteDisco {

    private static final int TAMANHO_BLOCO_SEQUENCIAL = 1024 * 1024; // 1 MB
    private static final int TAMANHO_BLOCO_ALEATORIO = 4096; // 4 KB
    private static final long TAMANHO_ARQUIVO = 100L * 1024 * 1024; // 100 MB

    private final AtomicBoolean emExecucao;
    private ExecutorService poolEstresse;
    private ScheduledExecutorService agendadorProgresso;

    public ServicoTesteDisco() {
        this.emExecucao = new AtomicBoolean(false);
    }

    public boolean estaEmExecucao() {
        return this.emExecucao.get();
    }

    public ResultadoTesteDisco executarTeste(
            ResultadoTesteDisco.TipoTesteDisco tipoTeste,
            int duracaoSegundos,
            Consumer<Integer> consumidorProgresso
    ) throws InterruptedException, IOException {

        if (this.emExecucao.get()) {
            throw new IllegalStateException("Teste de disco ja em execucao");
        }

        this.emExecucao.set(true);
        Instant instanteInicio = Instant.now();

        File arquivoTemporario = new File("guidepc_disco_teste.tmp");
        try {
            // Criar arquivo de teste
            this.criarArquivoTeste(arquivoTemporario);

            long inicioBenchmark = System.nanoTime();
            long totalBytesOperacao = 0;

            switch (tipoTeste) {
                case LEITURA_SEQUENCIAL -> totalBytesOperacao = this.executarLeituraSequencial(arquivoTemporario, duracaoSegundos, consumidorProgresso);
                case ESCRITA_SEQUENCIAL -> totalBytesOperacao = this.executarEscritaSequencial(arquivoTemporario, duracaoSegundos, consumidorProgresso);
                case LEITURA_ALEATORIA -> totalBytesOperacao = this.executarLeituraAleatoria(arquivoTemporario, duracaoSegundos, consumidorProgresso);
                case ESCRITA_ALEATORIA -> totalBytesOperacao = this.executarEscritaAleatoria(arquivoTemporario, duracaoSegundos, consumidorProgresso);
            }

            long fimBenchmark = System.nanoTime();
            double tempoTotalMs = (fimBenchmark - inicioBenchmark) / 1_000_000.0;
            double throughputBytesSeg = (tempoTotalMs > 0) ? (totalBytesOperacao * 1000.0 / tempoTotalMs) : 0;

            int tamanhoBloco = (tipoTeste == ResultadoTesteDisco.TipoTesteDisco.LEITURA_ALEATORIA
                    || tipoTeste == ResultadoTesteDisco.TipoTesteDisco.ESCRITA_ALEATORIA)
                    ? TAMANHO_BLOCO_ALEATORIO : TAMANHO_BLOCO_SEQUENCIAL;
            double iops = (tempoTotalMs > 0) ? (totalBytesOperacao / tamanhoBloco * 1000.0 / tempoTotalMs) : 0;

            Instant instanteFim = Instant.now();

            return new ResultadoTesteDisco(
                    tipoTeste, duracaoSegundos, instanteInicio, instanteFim,
                    TAMANHO_ARQUIVO, throughputBytesSeg, tempoTotalMs, iops
            );
        } finally {
            this.emExecucao.set(false);
            if (arquivoTemporario.exists()) {
                arquivoTemporario.delete();
            }
        }
    }

    public void solicitarParada() {
        this.emExecucao.set(false);
    }

    private void criarArquivoTeste(File arquivo) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(arquivo, "rw")) {
            raf.setLength(TAMANHO_ARQUIVO);
            // Escrever dados aleatorios para garantir que o arquivo nao seja compactado
            raf.seek(0);
            byte[] buffer = new byte[TAMANHO_BLOCO_SEQUENCIAL];
            ThreadLocalRandom.current().nextBytes(buffer);
            for (long escritos = 0; escritos < TAMANHO_ARQUIVO; escritos += buffer.length) {
                raf.write(buffer);
            }
        }
    }

    private long executarLeituraSequencial(File arquivo, int duracaoSegundos, Consumer<Integer> consumidorProgresso) throws InterruptedException {
        long totalLido = 0;
        long prazoFinalMillis = System.currentTimeMillis() + duracaoSegundos * 1000L;
        byte[] buffer = new byte[TAMANHO_BLOCO_SEQUENCIAL];

        try (RandomAccessFile raf = new RandomAccessFile(arquivo, "r");
             FileChannel canal = raf.getChannel()) {

            for (int segundos = 0; segundos <= duracaoSegundos && this.emExecucao.get(); segundos++) {
                long restanteMillis = prazoFinalMillis - System.currentTimeMillis();
                if (restanteMillis <= 0) break;

                int progresso = (int) (segundos * 100.0 / duracaoSegundos);
                if (consumidorProgresso != null) consumidorProgresso.accept(Math.min(progresso, 100));

                long inicioSegundo = System.currentTimeMillis();
                while (System.currentTimeMillis() - inicioSegundo < 1000 && this.emExecucao.get()) {
                    if (canal.position() >= TAMANHO_ARQUIVO) {
                        canal.position(0);
                    }
                    int lidos = canal.read(ByteBuffer.wrap(buffer));
                    if (lidos > 0) totalLido += lidos;
                }
            }
        } catch (IOException ignored) {}

        if (consumidorProgresso != null) consumidorProgresso.accept(100);
        return totalLido;
    }

    private long executarEscritaSequencial(File arquivo, int duracaoSegundos, Consumer<Integer> consumidorProgresso) throws InterruptedException {
        long totalEscrito = 0;
        long prazoFinalMillis = System.currentTimeMillis() + duracaoSegundos * 1000L;
        byte[] buffer = new byte[TAMANHO_BLOCO_SEQUENCIAL];
        ThreadLocalRandom.current().nextBytes(buffer);

        try (RandomAccessFile raf = new RandomAccessFile(arquivo, "rw");
             FileChannel canal = raf.getChannel()) {

            for (int segundos = 0; segundos <= duracaoSegundos && this.emExecucao.get(); segundos++) {
                long restanteMillis = prazoFinalMillis - System.currentTimeMillis();
                if (restanteMillis <= 0) break;

                int progresso = (int) (segundos * 100.0 / duracaoSegundos);
                if (consumidorProgresso != null) consumidorProgresso.accept(Math.min(progresso, 100));

                long inicioSegundo = System.currentTimeMillis();
                while (System.currentTimeMillis() - inicioSegundo < 1000 && this.emExecucao.get()) {
                    if (canal.position() >= TAMANHO_ARQUIVO) {
                        canal.position(0);
                    }
                    int escritos = canal.write(ByteBuffer.wrap(buffer));
                    if (escritos > 0) totalEscrito += escritos;
                }
                canal.force(false);
            }
        } catch (IOException ignored) {}

        if (consumidorProgresso != null) consumidorProgresso.accept(100);
        return totalEscrito;
    }

    private long executarLeituraAleatoria(File arquivo, int duracaoSegundos, Consumer<Integer> consumidorProgresso) throws InterruptedException {
        long totalLido = 0;
        long prazoFinalMillis = System.currentTimeMillis() + duracaoSegundos * 1000L;
        byte[] buffer = new byte[TAMANHO_BLOCO_ALEATORIO];
        long posicoesPossiveis = TAMANHO_ARQUIVO / TAMANHO_BLOCO_ALEATORIO;

        try (RandomAccessFile raf = new RandomAccessFile(arquivo, "r");
             FileChannel canal = raf.getChannel()) {

            for (int segundos = 0; segundos <= duracaoSegundos && this.emExecucao.get(); segundos++) {
                long restanteMillis = prazoFinalMillis - System.currentTimeMillis();
                if (restanteMillis <= 0) break;

                int progresso = (int) (segundos * 100.0 / duracaoSegundos);
                if (consumidorProgresso != null) consumidorProgresso.accept(Math.min(progresso, 100));

                long inicioSegundo = System.currentTimeMillis();
                while (System.currentTimeMillis() - inicioSegundo < 1000 && this.emExecucao.get()) {
                    long posicao = ThreadLocalRandom.current().nextLong(posicoesPossiveis) * TAMANHO_BLOCO_ALEATORIO;
                    canal.position(posicao);
                    int lidos = canal.read(ByteBuffer.wrap(buffer));
                    if (lidos > 0) totalLido += lidos;
                }
            }
        } catch (IOException ignored) {}

        if (consumidorProgresso != null) consumidorProgresso.accept(100);
        return totalLido;
    }

    private long executarEscritaAleatoria(File arquivo, int duracaoSegundos, Consumer<Integer> consumidorProgresso) throws InterruptedException {
        long totalEscrito = 0;
        long prazoFinalMillis = System.currentTimeMillis() + duracaoSegundos * 1000L;
        byte[] buffer = new byte[TAMANHO_BLOCO_ALEATORIO];
        ThreadLocalRandom.current().nextBytes(buffer);
        long posicoesPossiveis = TAMANHO_ARQUIVO / TAMANHO_BLOCO_ALEATORIO;

        try (RandomAccessFile raf = new RandomAccessFile(arquivo, "rw");
             FileChannel canal = raf.getChannel()) {

            for (int segundos = 0; segundos <= duracaoSegundos && this.emExecucao.get(); segundos++) {
                long restanteMillis = prazoFinalMillis - System.currentTimeMillis();
                if (restanteMillis <= 0) break;

                int progresso = (int) (segundos * 100.0 / duracaoSegundos);
                if (consumidorProgresso != null) consumidorProgresso.accept(Math.min(progresso, 100));

                long inicioSegundo = System.currentTimeMillis();
                while (System.currentTimeMillis() - inicioSegundo < 1000 && this.emExecucao.get()) {
                    long posicao = ThreadLocalRandom.current().nextLong(posicoesPossiveis) * TAMANHO_BLOCO_ALEATORIO;
                    canal.position(posicao);
                    int escritos = canal.write(ByteBuffer.wrap(buffer));
                    if (escritos > 0) totalEscrito += escritos;
                }
                canal.force(false);
            }
        } catch (IOException ignored) {}

        if (consumidorProgresso != null) consumidorProgresso.accept(100);
        return totalEscrito;
    }
}
