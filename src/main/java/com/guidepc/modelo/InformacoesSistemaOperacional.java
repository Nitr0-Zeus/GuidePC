package com.guidepc.modelo;

/**
 * InformacoesSistemaOperacional - Modelo imutavel. Campos mapeados do OSHI, usados em Servico e Visao.
 */
public record InformacoesSistemaOperacional(
        String familia,
        String versao,
        String codinome,
        String numeroBuild,
        String arquitetura,
        long tempoAtividadeSegundos,
        boolean elevado
) {
}
