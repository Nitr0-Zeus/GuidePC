package com.guidepc.modelo;

/** Dados da placa-mae e BIOS. Todos os campos ja vem sanitizados ("Nao disponivel" quando ausente). */
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
