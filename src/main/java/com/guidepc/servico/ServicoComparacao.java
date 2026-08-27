package com.guidepc.servico;

import com.guidepc.modelo.NivelEstresse;
import com.guidepc.modelo.ResultadoTesteEstresse;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * ServicoComparacao - Regra de negocio. Thread-safe onde acessa hardware.
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
        boolean possuiNormal = this.mapaResultados.containsKey(NivelEstresse.NORMAL);
        boolean possuiBaixo = this.mapaResultados.containsKey(NivelEstresse.BAIXO);
        boolean possuiAlto = this.mapaResultados.containsKey(NivelEstresse.ALTO);
        return possuiNormal && possuiBaixo && possuiAlto;
    }

    public String exportarCsv() {
        StringBuilder construtorTexto = new StringBuilder();
        construtorTexto.append("nivel,duracao_s,amostras,media_cpu,max_cpu,min_cpu,desvio_cpu,media_memoria,max_memoria,media_resposta_ms,max_resposta_ms,selo,estimativa_prox_cpu\n");
        for (NivelEstresse nivelEstresse : NivelEstresse.values()) {
            ResultadoTesteEstresse resultado = this.mapaResultados.get(nivelEstresse);
            java.util.Optional.ofNullable(resultado).ifPresent(resultadoPresente -> {
                String linhaCsv = String.format(Locale.US, "%s,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s,%.2f\n",
                        nivelEstresse.name(),
                        resultadoPresente.obterDuracaoSegundos(),
                        resultadoPresente.obterQuantidadeAmostras(),
                        resultadoPresente.obterMediaCpu(),
                        resultadoPresente.obterMaximoCpu(),
                        resultadoPresente.obterMinimoCpu(),
                        resultadoPresente.obterDesvioPadraoCpu(),
                        resultadoPresente.obterMediaMemoria(),
                        resultadoPresente.obterMaximoMemoria(),
                        resultadoPresente.obterMediaTempoRespostaMs(),
                        resultadoPresente.obterMaximoTempoRespostaMs(),
                        resultadoPresente.obterSeloDesempenho(),
                        resultadoPresente.estimarProximaCpu());
                construtorTexto.append(linhaCsv);
            });
        }
        return construtorTexto.toString();
    }

    public String gerarTextoResumo() {
        boolean mapaVazio = this.mapaResultados.isEmpty();
        switch (Boolean.toString(mapaVazio)) {
            case "true" -> {
                return "Nenhum teste executado ainda.";
            }
            default -> {
                // continua
            }
        }

        StringBuilder construtorResumo = new StringBuilder();
        for (NivelEstresse nivelEstresse : NivelEstresse.values()) {
            ResultadoTesteEstresse resultado = this.mapaResultados.get(nivelEstresse);
            java.util.Optional.ofNullable(resultado).ifPresent(resultadoPresente -> {
                String linhaResumo = String.format(Locale.US, "%s: CPU %.1f%% (pico %.1f%%) MEM %.1f%% Resp %.1fms [%s]\n",
                        nivelEstresse.obterRotulo(),
                        resultadoPresente.obterMediaCpu(),
                        resultadoPresente.obterMaximoCpu(),
                        resultadoPresente.obterMediaMemoria(),
                        resultadoPresente.obterMediaTempoRespostaMs(),
                        resultadoPresente.obterSeloDesempenho());
                construtorResumo.append(linhaResumo);
            });
        }

        switch (Boolean.toString(this.possuiTodosOsTresNiveis())) {
            case "true" -> {
                double mediaNormal = this.mapaResultados.get(NivelEstresse.NORMAL).obterMediaCpu();
                double mediaAlto = this.mapaResultados.get(NivelEstresse.ALTO).obterMediaCpu();
                double delta = mediaAlto - mediaNormal;
                String linhaDelta = String.format(Locale.US, "\nDelta Normal->Alto: +%.1f%% CPU. ", delta);
                construtorResumo.append(linhaDelta);
                String analiseDelta = switch (Boolean.toString(delta > 50.0)) {
                    case "true" -> "Sistema atinge carga maxima facilmente - bom para validar estabilidade.";
                    default -> "Variacao moderada - verifique resfriamento se pico > 90%.";
                };
                construtorResumo.append(analiseDelta);
            }
            default -> {
            }
        }

        construtorResumo.append("\n\nNota: estimativa 'proximo' e regressao linear simples, nao e predicao com inteligencia artificial.");
        return construtorResumo.toString();
    }
}
