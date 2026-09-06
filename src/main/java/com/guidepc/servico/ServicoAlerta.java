package com.guidepc.servico;

import com.guidepc.modelo.Alerta;
import com.guidepc.modelo.Amostra;
import com.guidepc.modelo.ConfiguracaoAlerta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servico de verificacao de alertas durante testes.
 * Compara cada amostra com os limites configurados e gera alertas quando excedidos.
 */
public class ServicoAlerta {

    private final ConfiguracaoAlerta configuracao;
    private final List<Alerta> alertasDisparados;

    public ServicoAlerta() {
        this.configuracao = new ConfiguracaoAlerta();
        this.alertasDisparados = new ArrayList<>();
    }

    public ConfiguracaoAlerta obterConfiguracao() {
        return this.configuracao;
    }

    public List<Alerta> obterAlertas() {
        return List.copyOf(this.alertasDisparados);
    }

    public void limparAlertas() {
        this.alertasDisparados.clear();
    }

    /**
     * Verifica a amostra e retorna alertas disparados.
     */
    public List<Alerta> verificar(Amostra amostra) {
        List<Alerta> novosAlertas = new ArrayList<>();

        if (!this.configuracao.estaHabilitado()) {
            return novosAlertas;
        }

        long agora = System.currentTimeMillis();

        // Alerta de CPU
        if (!Double.isNaN(amostra.cargaCpuPercentual())
                && amostra.cargaCpuPercentual() > this.configuracao.obterLimiteCpu()) {
            Alerta alerta = new Alerta(Alerta.TipoAlerta.CPU,
                    amostra.cargaCpuPercentual(),
                    this.configuracao.obterLimiteCpu(), agora);
            novosAlertas.add(alerta);
        }

        // Alerta de temperatura
        if (!Double.isNaN(amostra.temperaturaCelsius())
                && amostra.temperaturaCelsius() > this.configuracao.obterLimiteTemperatura()) {
            Alerta alerta = new Alerta(Alerta.TipoAlerta.TEMPERATURA,
                    amostra.temperaturaCelsius(),
                    this.configuracao.obterLimiteTemperatura(), agora);
            novosAlertas.add(alerta);
        }

        // Alerta de memoria
        if (!Double.isNaN(amostra.usoMemoriaPercentual())
                && amostra.usoMemoriaPercentual() > this.configuracao.obterLimiteMemoria()) {
            Alerta alerta = new Alerta(Alerta.TipoAlerta.MEMORIA,
                    amostra.usoMemoriaPercentual(),
                    this.configuracao.obterLimiteMemoria(), agora);
            novosAlertas.add(alerta);
        }

        this.alertasDisparados.addAll(novosAlertas);
        return novosAlertas;
    }
}
