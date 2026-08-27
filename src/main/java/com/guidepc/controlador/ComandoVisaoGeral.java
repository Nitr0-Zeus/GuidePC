package com.guidepc.controlador;

import com.guidepc.modelo.InformacoesHardware;
import com.guidepc.servico.ServicoColetorHardware;
import com.guidepc.visao.VisaoGeralConsole;

/**
 * ComandoVisaoGeral - Acao do menu via padrao Comando. Evita if-else no Principal.
 */
public final class ComandoVisaoGeral implements Comando {

    private final ServicoColetorHardware servicoColetorHardware;

    public ComandoVisaoGeral(ServicoColetorHardware servicoColetorHardware) {
        this.servicoColetorHardware = servicoColetorHardware;
    }

    @Override
    public void executar() {
        InformacoesHardware informacoesHardware = this.servicoColetorHardware.coletarTudo();
        VisaoGeralConsole.exibir(informacoesHardware);
    }
}
