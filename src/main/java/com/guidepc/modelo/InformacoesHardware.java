package com.guidepc.modelo;

import java.util.List;

/**
 * InformacoesHardware - Modelo imutavel. Campos mapeados do OSHI, usados em Servico e Visao.
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
