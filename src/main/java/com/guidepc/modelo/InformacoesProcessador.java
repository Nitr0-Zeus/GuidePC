package com.guidepc.modelo;

/**
 * Dados do processador mapeados do OSHI.
 * Frequencias em Hz; percentual em 0..100; temperatura em Celsius ou NaN.
 *
 * @param fabricante          fabricante do processador (ex.: "Intel", "AMD")
 * @param modelo              modelo completo do processador
 * @param microarquitetura    arquitetura do processador (ex.: "x86_64", "ARM")
 * @param nucleosFisicos      quantidade de nucleos fisicos
 * @param nucleosLogicos      quantidade de nucleos logicos (com hyper-threading)
 * @param pacotesFisicos      numero de pacotes (sockets) fisicos
 * @param frequenciaBaseHz    frequencia base em Hertz
 * @param frequenciaMaximaHz  frequencia maxima (turbo/boost) em Hertz
 * @param frequenciasAtuaisHz array com as frequencias atuais de cada nucleo/logico em Hz
 * @param percentualUso       uso total do CPU em percentual (0.0 a 100.0)
 * @param temperaturaCelsius  temperatura em graus Celsius ou Double.NaN se indisponivel
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
