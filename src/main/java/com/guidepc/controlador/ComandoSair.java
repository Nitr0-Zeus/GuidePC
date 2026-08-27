package com.guidepc.controlador;

import com.guidepc.visao.VisaoConsole;

/**
 * ComandoSair - Acao do menu via padrao Comando. Evita if-else no Principal.
 */
public final class ComandoSair implements Comando {

    @Override
    public void executar() {
        VisaoConsole.exibirLinha("Encerrando GuidePC. Ate logo!");
        System.exit(0);
    }
}
