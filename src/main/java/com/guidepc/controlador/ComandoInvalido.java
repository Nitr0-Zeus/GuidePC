package com.guidepc.controlador;

import com.guidepc.visao.VisaoConsole;

/** Tratado quando o usuario digita opcao fora de 1..4. */
public final class ComandoInvalido implements Comando {

    @Override
    public void executar() {
        VisaoConsole.exibirErro("Opcao invalida. Escolha 1, 2, 3 ou 4.");
    }
}
