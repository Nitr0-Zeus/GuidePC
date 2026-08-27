package com.guidepc.servico;

import com.guidepc.modelo.NivelEstresse;
import com.guidepc.modelo.ResultadoTesteEstresse;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Armazena resultados por nivel e gera comparativo/exportacao.
 *
 * <p>Mapa interno e um EnumMap (eficiente para chaves enum). Nao e thread-safe
 * para escrita concorrente - o uso no app e sequencial via menu.</p>
 */
public class ServicoComparacao {

    private final Map<NivelEstresse, ResultadoTesteEstresse> mapaResultados;

    public ServicoComparacao() {
        this.mapaResultados = new EnumMap<>(NivelEstresse.class);
    }

    public void armazenarResultado(ResultadoTesteEstresse resultadoTesteEstresse) {
        this.mapaResultados.put(resultadoTesteEstresse.obterNivelEstresse(), resultadoTesteEstresse);
    }

    public Optional<ResultadoTesteEstresse> obterResultado(NivelEstresse nivelEstresse) {
        return Optional.ofNullable(this.mapaResultados.get(nivelEstresse));
    }

    public Map<NivelEstresse, ResultadoTesteEstresse> obterTodosResultados() {
        return Collections.unmodifiableMap(this.mapaResultados);
    }

    public void limparResultados() {
        this.mapaResultados.clear();
    }

    public boolean possuiTodosOsTresNiveis() {
        return this.mapaResultados.containsKey(NivelEstresse.NORMAL)
                && this.mapaResultados.containsKey(NivelEstresse.BAIXO)
                && this.mapaResultados.containsKey(NivelEstresse.ALTO);
    }

    /** Gera CSV com cabecalho + uma linha por nivel executado. */
    public String exportarCsv() {
        StringBuilder construtorTexto = new StringBuilder();
        construtorTexto.append("nivel,duracao_s,amostras,media_cpu,max_cpu,min_cpu,desvio_cpu,media_memoria,max_memoria,media_resposta_ms,max_resposta_ms,selo,estimativa_prox_cpu\n");
        for (NivelEstresse nivelEstresse : NivelEstresse.values()) {
            ResultadoTesteEstresse resultado = this.mapaResultados.get(nivelEstresse);
            if (resultado == null) {
                continue;
            }
            String linhaCsv = String.format(Locale.US, "%s,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s,%.2f\n",
                    nivelEstresse.name(),
                    resultado.obterDuracaoSegundos(),
                    resultado.obterQuantidadeAmostras(),
                    resultado.obterMediaCpu(),
                    resultado.obterMaximoCpu(),
                    resultado.obterMinimoCpu(),
                    resultado.obterDesvioPadraoCpu(),
                    resultado.obterMediaMemoria(),
                    resultado.obterMaximoMemoria(),
                    resultado.obterMediaTempoRespostaMs(),
                    resultado.obterMaximoTempoRespostaMs(),
                    resultado.obterSeloDesempenho(),
                    resultado.estimarProximaCpu());
            construtorTexto.append(linhaCsv);
        }
        return construtorTexto.toString();
    }

    /** Texto humano para a visao de comparativo, incluindo delta Normal->Alto quando houver os 3 niveis. */
    public String gerarTextoResumo() {
        if (this.mapaResultados.isEmpty()) {
            return "Nenhum teste executado ainda.";
        }

        StringBuilder construtorResumo = new StringBuilder();
        for (NivelEstresse nivelEstresse : NivelEstresse.values()) {
            ResultadoTesteEstresse resultado = this.mapaResultados.get(nivelEstresse);
            if (resultado == null) {
                continue;
            }
            String linhaResumo = String.format(Locale.US, "%s: CPU %.1f%% (pico %.1f%%) MEM %.1f%% Resp %.1fms [%s]\n",
                    nivelEstresse.obterRotulo(),
                    resultado.obterMediaCpu(),
                    resultado.obterMaximoCpu(),
                    resultado.obterMediaMemoria(),
                    resultado.obterMediaTempoRespostaMs(),
                    resultado.obterSeloDesempenho());
            construtorResumo.append(linhaResumo);
        }

        if (this.possuiTodosOsTresNiveis()) {
            double mediaNormal = this.mapaResultados.get(NivelEstresse.NORMAL).obterMediaCpu();
            double mediaAlto = this.mapaResultados.get(NivelEstresse.ALTO).obterMediaCpu();
            double delta = mediaAlto - mediaNormal;
            String linhaDelta = String.format(Locale.US, "\nDelta Normal->Alto: +%.1f%% CPU. ", delta);
            construtorResumo.append(linhaDelta);

            String analiseDelta = (delta > 50.0)
                    ? "Sistema atinge carga maxima facilmente - bom para validar estabilidade."
                    : "Variacao moderada - verifique resfriamento se pico > 90%.";
            construtorResumo.append(analiseDelta);
        }

        construtorResumo.append("\n\nNota: estimativa 'proximo' e regressao linear simples, nao e predicao com inteligencia artificial.");
        return construtorResumo.toString();
    }
}
