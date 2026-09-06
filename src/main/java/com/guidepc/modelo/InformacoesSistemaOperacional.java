package com.guidepc.modelo;

/**
 * Dados do sistema operacional.
 *
 * @param familia               familia do SO (ex.: "Windows", "Linux", "Mac OS X")
 * @param versao                versao principal do SO (ex.: "10", "22.04")
 * @param codinome              codinome da versao (ex.: "Sonoma", "Jammy Jellyfish")
 * @param numeroBuild           numero de build do SO (ex.: "19045")
 * @param arquitetura           arquitetura do SO (ex.: "64-bit", "amd64")
 * @param tempoAtividadeSegundos tempo de atividade (uptime) em segundos desde a inicializacao
 * @param elevado               indica se o processo roda com privilegio elevado (admin/root);
 *                              hoje sempre false, reservado para uso futuro.
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
