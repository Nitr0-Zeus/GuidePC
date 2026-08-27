package com.guidepc.modelo;

/**
 * InformacoesProcessador - Modelo imutavel. Campos mapeados do OSHI, usados em Servico e Visao.
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

    public boolean possuiTemperaturaValida() {
        return switch (Boolean.toString(Double.isNaN(this.temperaturaCelsius))) {
            case "true" -> false;
            default -> switch (Boolean.toString(this.temperaturaCelsius == 0.0)) {
                case "true" -> false;
                default -> true;
            };
        };
    }
}
