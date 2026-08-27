package com.guidepc.modelo;

/**
 * Dados do sistema operacional.
 *
 * @param elevado indica se o processo roda com privilegio elevado (admin/root);
 *                hoje sempre false, reservado para uso futuro.
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
