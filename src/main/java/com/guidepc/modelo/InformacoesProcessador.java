package com.guidepc.modelo;

/**
 * Dados do processador mapeados do OSHI.
 * Frequencias em Hz; percentual em 0..100; temperatura em Celsius ou NaN.
 */
public record InformacoesProcessador(
        String fabricante,
        String modelo,
        String microarquitetura,
        int nucleosFisicos,
        int nucleosLogicos,
        int pacotesFisicos,
        long frequenciaBaseHz,
        long frequenciaMaximaHz,
        long[] frequenciasAtuaisHz,
        double percentualUso,
        double temperaturaCelsius
) {

    /**
     * Indica se a leitura de temperatura e confiavel.
     * OSHI retorna 0.0 ou NaN quando nao ha sensor/driver.
     */
    public boolean possuiTemperaturaValida() {
        return !Double.isNaN(this.temperaturaCelsius) && this.temperaturaCelsius != 0.0;
    }
}
