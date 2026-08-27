package com.guidepc.visao;

import com.guidepc.modelo.NivelEstresse;
import com.guidepc.modelo.ResultadoTesteEstresse;
import com.guidepc.utilitario.Formatador;

import java.util.Locale;
import java.util.Map;

/**
 * VisaoComparativoConsole - Impressao console. Tabela ASCII e barra de progresso.
 */
public final class VisaoComparativoConsole {

    private VisaoComparativoConsole() {
    }

    public static void exibir(Map<NivelEstresse, ResultadoTesteEstresse> mapaResultados, String textoResumo) {
        VisaoConsole.exibirCabecalho("COMPARATIVO - ATUAL VS PROXIMO (ESTIMATIVA)");
        VisaoConsole.exibirLinha("Nota: estimativa 'proximo' e regressao linear simples, nao e inteligencia artificial");
        VisaoConsole.exibirSeparador();

        boolean mapaVazio = mapaResultados.isEmpty();
        switch (Boolean.toString(mapaVazio)) {
            case "true" -> {
                VisaoConsole.exibirLinha("Nenhum teste executado ainda. Execute Normal, Baixo e Alto no menu Teste.");
                return;
            }
            default -> {
            }
        }

        // Cabecalho tabela ASCII
        String cabecalhoTabela = String.format(Locale.US, "%-12s | %-10s | %-10s | %-10s | %-12s | %-8s",
                "NIVEL", "MEDIA CPU", "PICO CPU", "MEDIA RAM", "RESP MEDIA", "SELO");
        VisaoConsole.exibirLinha(cabecalhoTabela);
        VisaoConsole.exibirLinha("--------------------------------------------------------------------------------");

        for (NivelEstresse nivelEstresse : NivelEstresse.values()) {
            ResultadoTesteEstresse resultado = mapaResultados.get(nivelEstresse);
            java.util.Optional.ofNullable(resultado).ifPresent(resultadoPresente -> {
                String linhaTabela = String.format(Locale.US, "%-12s | %-10s | %-10s | %-10s | %-12s | %-8s",
                        nivelEstresse.name(),
                        Formatador.formatarPercentual(resultadoPresente.obterMediaCpu()),
                        Formatador.formatarPercentual(resultadoPresente.obterMaximoCpu()),
                        Formatador.formatarPercentual(resultadoPresente.obterMediaMemoria()),
                        Formatador.formatarTempoMs(resultadoPresente.obterMediaTempoRespostaMs()),
                        resultadoPresente.obterSeloDesempenho());
                VisaoConsole.exibirLinha(linhaTabela);

                String linhaEstimativa = String.format(Locale.US, "  -> Estimativa prox CPU: %s | Amostras: %d | Desvio CPU: %.2f",
                        Formatador.formatarPercentual(resultadoPresente.estimarProximaCpu()),
                        resultadoPresente.obterQuantidadeAmostras(),
                        resultadoPresente.obterDesvioPadraoCpu());
                VisaoConsole.exibirLinha(linhaEstimativa);
            });
        }

        VisaoConsole.exibirSeparador();
        VisaoConsole.exibirLinha("[RESUMO]");
        VisaoConsole.exibirLinha(textoResumo);
        VisaoConsole.exibirSeparador();
    }
}
