package com.guidepc.modelo;

/**
 * Espaco em disco de uma particao/montagem.
 *
 * @param pontoMontagem nome ou ponto de montagem (ex: C:\, /home)
 * @param totalBytes    espaco total em bytes
 * @param usadoBytes    espaco utilizado em bytes
 * @param livreBytes    espacoutilizavel em bytes
 */
public record InformacoesDiscoParticao(
        String pontoMontagem,
        long totalBytes,
        long usadoBytes,
        long livreBytes
) {
}
