package com.guidepc.modelo;

import java.time.Instant;

/**
 * Resultado de um teste de disco.
 */
public record ResultadoTesteDisco(
        TipoTesteDisco tipoTeste,
        int duracaoSegundos,
        Instant instanteInicio,
        Instant instanteFim,
        long tamanhoArquivoBytes,
        double throughputBytesSeg,
        double tempoTotalMs,
        double iops
) {

    public enum TipoTesteDisco {
        LEITURA_SEQUENCIAL("Leitura Sequencial", "Le bloco sequencial de 1 MB"),
        ESCRITA_SEQUENCIAL("Escrita Sequencial", "Escreve bloco sequencial de 1 MB"),
        LEITURA_ALEATORIA("Leitura Aleatoria", "Le blocos aleatorios de 4 KB"),
        ESCRITA_ALEATORIA("Escrita Aleatoria", "Escreve blocos aleatorios de 4 KB");

        private final String rotulo;
        private final String descricao;

        TipoTesteDisco(String rotulo, String descricao) {
            this.rotulo = rotulo;
            this.descricao = descricao;
        }

        public String obterRotulo() {
            return this.rotulo;
        }

        public String obterDescricao() {
            return this.descricao;
        }
    }

    public double obterThroughputMBs() {
        return this.throughputBytesSeg / (1024.0 * 1024.0);
    }
}
