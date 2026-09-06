package com.guidepc.modelo;

/**
 * Dados da placa-mae e BIOS. Todos os campos ja vem sanitizados ("Nao disponivel" quando ausente).
 *
 * @param fabricante          fabricante da placa-mae
 * @param modelo              modelo da placa-mae
 * @param versao              versao/revisao da placa-mae
 * @param numeroSerial        numero de serie da placa-mae
 * @param fabricanteBios      fabricante do firmware BIOS/UEFI
 * @param versaoBios          versao do BIOS/UEFI
 * @param dataLancamentoBios  data de lancamento do BIOS (formato do SO)
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
