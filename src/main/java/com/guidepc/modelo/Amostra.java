package com.guidepc.modelo;

/**
 * Amostra - Metrica instantanea a cada 500ms durante teste.
 */
public record Amostra(
        long instanteMillis,
        double cargaCpuPercentual,
        double usoMemoriaPercentual,
        long frequenciaCpuHz,
        double temperaturaCelsius,
        double tempoRespostaMs
) {
}
