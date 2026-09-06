package com.guidepc.modelo;

/**
 * Configuracao de limites para alertas durante testes.
 */
public class ConfiguracaoAlerta {

    private double limiteCpuPercentual;
    private double limiteTemperaturaCelsius;
    private double limiteMemoriaPercentual;
    private boolean habilitado;

    public ConfiguracaoAlerta() {
        this.limiteCpuPercentual = 90.0;
        this.limiteTemperaturaCelsius = 85.0;
        this.limiteMemoriaPercentual = 90.0;
        this.habilitado = true;
    }

    public double obterLimiteCpu() {
        return this.limiteCpuPercentual;
    }

    public void definirLimiteCpu(double limite) {
        this.limiteCpuPercentual = Math.max(10.0, Math.min(100.0, limite));
    }

    public double obterLimiteTemperatura() {
        return this.limiteTemperaturaCelsius;
    }

    public void definirLimiteTemperatura(double limite) {
        this.limiteTemperaturaCelsius = Math.max(30.0, Math.min(110.0, limite));
    }

    public double obterLimiteMemoria() {
        return this.limiteMemoriaPercentual;
    }

    public void definirLimiteMemoria(double limite) {
        this.limiteMemoriaPercentual = Math.max(10.0, Math.min(100.0, limite));
    }

    public boolean estaHabilitado() {
        return this.habilitado;
    }

    public void definirHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    @Override
    public String toString() {
        return String.format("CPU: %.0f%% | Temp: %.0f C | RAM: %.0f%% [%s]",
                this.limiteCpuPercentual, this.limiteTemperaturaCelsius,
                this.limiteMemoriaPercentual, this.habilitado ? "ATIVO" : "INATIVO");
    }
}
