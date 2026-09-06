package com.guidepc.modelo;

import java.util.List;

/**
 * Agregado imutavel com todo o hardware coletado via OSHI.
 * Usado pela API REST para retornar dados do sistema ao frontend.
 */
public record InformacoesHardware(
        InformacoesProcessador processador,
        InformacoesMemoria memoria,
        List<InformacoesDisco> discos,
        InformacoesPlacaMae placaMae,
        InformacoesSistemaOperacional sistemaOperacional,
        List<String> nomesGpu
) {
}
