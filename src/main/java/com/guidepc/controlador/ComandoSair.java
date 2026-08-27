package com.guidepc.controlador;

import com.guidepc.visao.VisaoConsole;

/** Opcao 4 do menu: encerra a aplicacao. */
public final class ComandoSair implements Comando {

    @Override
    public void executar() {
        VisaoConsole.exibirLinha("Encerrando GuidePC. Ate logo!");
        System.exit(0);
    }
}
