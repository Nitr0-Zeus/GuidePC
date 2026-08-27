package com.guidepc.modelo;

/**
 * Amostra coletada a cada ~500 ms durante o teste de estresse.
 *
 * @param instanteMillis       timestamp da coleta (epoch millis)
 * @param cargaCpuPercentual   uso de CPU no instante (0 a 100)
 * @param usoMemoriaPercentual uso de RAM no instante (0 a 100)
 * @param frequenciaCpuHz      frequencia maxima observada entre os nucleos (Hz)
 * @param temperaturaCelsius   temperatura da CPU se houver sensor; NaN caso indisponivel
 * @param tempoRespostaMs      duracao do micro-benchmark interno (sin/cos), usado como proxy de responsividade
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
