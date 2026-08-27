package com.guidepc.modelo;

/**
 * InformacoesMemoria - Modelo imutavel. Campos mapeados do OSHI, usados em Servico e Visao.
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
