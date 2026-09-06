package com.guidepc.servico;

import com.guidepc.modelo.ConfiguracaoAlerta;

/**
 * Serviço singleton de monitoramento — fornece acesso à configuração de alertas.
 */
public final class ServicoMonitoramento {

    private static final ServicoMonitoramento INSTANCIA = new ServicoMonitoramento();
    private final ServicoAlerta servicoAlerta;

    private ServicoMonitoramento() {
        this.servicoAlerta = new ServicoAlerta();
    }

    public static ServicoMonitoramento obterInstancia() {
        return INSTANCIA;
    }

    public ConfiguracaoAlerta getConfiguracaoAlerta() {
        return servicoAlerta.obterConfiguracao();
    }

    public ServicoAlerta obterServicoAlerta() {
        return servicoAlerta;
    }
}
