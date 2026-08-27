package com.guidepc.controlador;

import com.guidepc.visao.VisaoConsole;

/**
 * ComandoInvalido - Acao do menu via padrao Comando. Evita if-else no Principal.
 */
public final class ComandoInvalido implements Comando {

    @Override
    public void executar() {
        VisaoConsole.exibirErro("Opcao invalida. Escolha 1, 2, 3 ou 4.");
    }
}
