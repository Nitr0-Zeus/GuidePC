package com.guidepc.modelo;

/**
 * Dados da bateria do sistema (notebooks).
 *
 * @param percentual           percentual de carga (0-100), -1 se desconhecido
 * @param carregando           true se conectado a fonte de alimentacao
 * @param tempoRestanteSegundos tempo estimado restante em segundos, -1 se desconhecido
 */
public record InformacoesBateria(
        double percentual,
        boolean carregando,
        long tempoRestanteSegundos
) {
}
