package com.guidepc.modelo;

/**
 * Snapshot da memoria RAM no momento da coleta.
 *
 * @param totalBytes                capacidade total instalada
 * @param disponivelBytes           memoria disponivel para alocacao
 * @param emUsoBytes                total - disponivel
 * @param percentualUso             emUso / total * 100
 * @param tamanhoPaginaBytes        tamanho da pagina do SO
 * @param informacoesMemoriaVirtual descricao do SO sobre memoria virtual/swap
 */
public record InformacoesMemoria(
        long totalBytes,
        long disponivelBytes,
        long emUsoBytes,
        double percentualUso,
        long tamanhoPaginaBytes,
        String informacoesMemoriaVirtual
) {
}
