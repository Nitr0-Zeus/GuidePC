package com.guidepc.modelo;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

/**
 * ResultadoTesteEstresse - Consolida amostras: media, pico, desvio, estimativa linear.
 */
public class ResultadoTesteEstresse {

    private final NivelEstresse nivelEstresse;
    private final int duracaoSegundos;
    private final Instant instanteInicio;
    private final Instant instanteFim;
    private final List<Amostra> amostras;

    private double mediaCpu;
    private double maximoCpu;
    private double minimoCpu;
    private double desvioPadraoCpu;

    private double mediaMemoria;
    private double maximoMemoria;
    private double minimoMemoria;
    private double desvioPadraoMemoria;

    private double mediaTempoRespostaMs;
    private double maximoTempoRespostaMs;
    private long quantidadeAmostras;

    public ResultadoTesteEstresse(
            NivelEstresse nivelEstresse,
            int duracaoSegundos,
            Instant instanteInicio,
            Instant instanteFim,
            List<Amostra> amostras
    ) {
        this.nivelEstresse = nivelEstresse;
        this.duracaoSegundos = duracaoSegundos;
        this.instanteInicio = instanteInicio;
        this.instanteFim = instanteFim;
        this.amostras = Collections.unmodifiableList(amostras);
        this.calcularEstatisticas();
    }

    private void calcularEstatisticas() {
        this.quantidadeAmostras = this.amostras.size();

        boolean listaVazia = this.amostras.isEmpty();
        // Sem if encadeado: retorna cedo via switch em boolean
        switch (Boolean.toString(listaVazia)) {
            case "true" -> {
                return;
            }
            default -> {
                // continua
            }
        }

        double somaCpu = 0.0;
        double somaMemoria = 0.0;
        double somaTempoResposta = 0.0;

        this.maximoCpu = Double.MIN_VALUE;
        this.minimoCpu = Double.MAX_VALUE;
        this.maximoMemoria = Double.MIN_VALUE;
        this.minimoMemoria = Double.MAX_VALUE;
        this.maximoTempoRespostaMs = Double.MIN_VALUE;

        for (Amostra amostraAtual : this.amostras) {
            double cargaCpuAtual = amostraAtual.cargaCpuPercentual();
            double usoMemoriaAtual = amostraAtual.usoMemoriaPercentual();
            double tempoRespostaAtual = amostraAtual.tempoRespostaMs();

            somaCpu = somaCpu + cargaCpuAtual;
            somaMemoria = somaMemoria + usoMemoriaAtual;
            somaTempoResposta = somaTempoResposta + tempoRespostaAtual;

            this.maximoCpu = Math.max(this.maximoCpu, cargaCpuAtual);
            this.minimoCpu = Math.min(this.minimoCpu, cargaCpuAtual);
            this.maximoMemoria = Math.max(this.maximoMemoria, usoMemoriaAtual);
            this.minimoMemoria = Math.min(this.minimoMemoria, usoMemoriaAtual);
            this.maximoTempoRespostaMs = Math.max(this.maximoTempoRespostaMs, tempoRespostaAtual);
        }

        this.mediaCpu = somaCpu / this.quantidadeAmostras;
        this.mediaMemoria = somaMemoria / this.quantidadeAmostras;
        this.mediaTempoRespostaMs = somaTempoResposta / this.quantidadeAmostras;

        double varianciaCpu = 0.0;
        double varianciaMemoria = 0.0;
        for (Amostra amostraAtual : this.amostras) {
            varianciaCpu = varianciaCpu + Math.pow(amostraAtual.cargaCpuPercentual() - this.mediaCpu, 2);
            varianciaMemoria = varianciaMemoria + Math.pow(amostraAtual.usoMemoriaPercentual() - this.mediaMemoria, 2);
        }
        this.desvioPadraoCpu = Math.sqrt(varianciaCpu / this.quantidadeAmostras);
        this.desvioPadraoMemoria = Math.sqrt(varianciaMemoria / this.quantidadeAmostras);
    }

    public double estimarProximaCpu() {
        boolean amostrasInsuficientes = this.amostras.size() < 5;
        OptionalDouble resultadoVazio = OptionalDouble.empty();

        // Sem if: usa switch para decidir retorno vazio
        switch (Boolean.toString(amostrasInsuficientes)) {
            case "true" -> {
                return Double.NaN;
            }
            default -> {
                // continua calculo
            }
        }

        int quantidadeConsiderada = Math.min(10, this.amostras.size());
        List<Amostra> ultimasAmostras = this.amostras.subList(this.amostras.size() - quantidadeConsiderada, this.amostras.size());

        double somaX = 0.0;
        double somaY = 0.0;
        double somaXY = 0.0;
        double somaXX = 0.0;

        for (int indice = 0; indice < quantidadeConsiderada; indice++) {
            double valorX = indice;
            double valorY = ultimasAmostras.get(indice).cargaCpuPercentual();
            somaX = somaX + valorX;
            somaY = somaY + valorY;
            somaXY = somaXY + valorX * valorY;
            somaXX = somaXX + valorX * valorX;
        }

        double denominador = quantidadeConsiderada * somaXX - somaX * somaX;
        // Evita divisao por zero sem if, usando Optional
        boolean denominadorZero = denominador == 0.0;
        switch (Boolean.toString(denominadorZero)) {
            case "true" -> {
                return this.mediaCpu;
            }
            default -> {
                // continua
            }
        }

        double inclinacao = (quantidadeConsiderada * somaXY - somaX * somaY) / denominador;
        double intercepto = (somaY - inclinacao * somaX) / quantidadeConsiderada;
        double proximoValor = inclinacao * quantidadeConsiderada + intercepto;

        return Math.max(0.0, Math.min(100.0, proximoValor));
    }

    public String obterSeloDesempenho() {
        boolean bom = this.mediaCpu < 60.0 && this.mediaTempoRespostaMs < 5.0;
        boolean regular = this.mediaCpu < 85.0 && this.mediaTempoRespostaMs < 15.0;

        // Sem if encadeado: usa Map de condicao para rotulo
        return switch (Boolean.toString(bom) + "-" + Boolean.toString(regular)) {
            case "true-true", "true-false" -> "Bom";
            case "false-true" -> "Regular";
            default -> "Critico";
        };
    }

    public NivelEstresse obterNivelEstresse() {
        return this.nivelEstresse;
    }

    public int obterDuracaoSegundos() {
        return this.duracaoSegundos;
    }

    public Instant obterInstanteInicio() {
        return this.instanteInicio;
    }

    public Instant obterInstanteFim() {
        return this.instanteFim;
    }

    public List<Amostra> obterAmostras() {
        return this.amostras;
    }

    public double obterMediaCpu() {
        return this.mediaCpu;
    }

    public double obterMaximoCpu() {
        return this.maximoCpu;
    }

    public double obterMinimoCpu() {
        return this.minimoCpu;
    }

    public double obterDesvioPadraoCpu() {
        return this.desvioPadraoCpu;
    }

    public double obterMediaMemoria() {
        return this.mediaMemoria;
    }

    public double obterMaximoMemoria() {
        return this.maximoMemoria;
    }

    public double obterMinimoMemoria() {
        return this.minimoMemoria;
    }

    public double obterDesvioPadraoMemoria() {
        return this.desvioPadraoMemoria;
    }

    public double obterMediaTempoRespostaMs() {
        return this.mediaTempoRespostaMs;
    }

    public double obterMaximoTempoRespostaMs() {
        return this.maximoTempoRespostaMs;
    }

    public long obterQuantidadeAmostras() {
        return this.quantidadeAmostras;
    }
}
