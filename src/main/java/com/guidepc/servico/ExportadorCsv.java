package com.guidepc.servico;

import com.guidepc.modelo.NivelEstresse;
import com.guidepc.modelo.ResultadoTesteEstresse;

import java.util.Locale;
import java.util.Map;

/**
 * Gera conteudo CSV a partir dos resultados armazenados.
 * Extraido de ServicoComparacao para manter responsabilidade unica
 * e ser reutilizado tanto no console quanto no PDF.
 */
public final class ExportadorCsv {

    private ExportadorCsv() {
    }

    public static String gerar(Map<NivelEstresse, ResultadoTesteEstresse> mapaResultados) {
        StringBuilder construtorTexto = new StringBuilder();
        construtorTexto.append("nivel,duracao_s,amostras,media_cpu,max_cpu,min_cpu,desvio_cpu,media_memoria,max_memoria,media_resposta_ms,max_resposta_ms,selo,estimativa_prox_cpu\n");
        for (NivelEstresse nivelEstresse : NivelEstresse.values()) {
            ResultadoTesteEstresse resultado = mapaResultados.get(nivelEstresse);
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
}
