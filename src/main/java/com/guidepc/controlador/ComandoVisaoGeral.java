package com.guidepc.controlador;

import com.guidepc.modelo.InformacoesHardware;
import com.guidepc.servico.ServicoColetorHardware;
import com.guidepc.visao.VisaoGeralConsole;

/** Opcao 1 do menu: coleta hardware via OSHI e imprime a visao geral. */
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
