package com.guidepc.modelo;

/**
 * InformacoesPlacaMae - Modelo imutavel. Campos mapeados do OSHI, usados em Servico e Visao.
 */
public record InformacoesPlacaMae(
        String fabricante,
        String modelo,
        String versao,
        String numeroSerial,
        String fabricanteBios,
        String versaoBios,
        String dataLancamentoBios
) {
}
