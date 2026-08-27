package com.guidepc.visao;

import com.guidepc.modelo.NivelEstresse;
import com.guidepc.modelo.ResultadoTesteEstresse;
import com.guidepc.utilitario.Formatador;

import java.util.Locale;
import java.util.Map;

/** Tabela ASCII do comparativo + resumo textual. */
public final class VisaoComparativoConsole {

    private VisaoComparativoConsole() {
    }

    public static void exibir(Map<NivelEstresse, ResultadoTesteEstresse> mapaResultados, String textoResumo) {
        VisaoConsole.exibirCabecalho("COMPARATIVO - ATUAL VS PROXIMO (ESTIMATIVA)");
        VisaoConsole.exibirLinha("Nota: estimativa 'proximo' e regressao linear simples, nao e inteligencia artificial");
        VisaoConsole.exibirSeparador();

        if (mapaResultados.isEmpty()) {
            VisaoConsole.exibirLinha("Nenhum teste executado ainda. Execute Normal, Baixo e Alto no menu Teste.");
            return;
        }

        String cabecalhoTabela = String.format(Locale.US, "%-12s | %-10s | %-10s | %-10s | %-12s | %-8s",
                "NIVEL", "MEDIA CPU", "PICO CPU", "MEDIA RAM", "RESP MEDIA", "SELO");
        VisaoConsole.exibirLinha(cabecalhoTabela);
        VisaoConsole.exibirLinha("--------------------------------------------------------------------------------");

        for (NivelEstresse nivelEstresse : NivelEstresse.values()) {
            ResultadoTesteEstresse resultado = mapaResultados.get(nivelEstresse);
            if (resultado == null) {
                continue;
            }
            String linhaTabela = String.format(Locale.US, "%-12s | %-10s | %-10s | %-10s | %-12s | %-8s",
                    nivelEstresse.name(),
                    Formatador.formatarPercentual(resultado.obterMediaCpu()),
                    Formatador.formatarPercentual(resultado.obterMaximoCpu()),
                    Formatador.formatarPercentual(resultado.obterMediaMemoria()),
                    Formatador.formatarTempoMs(resultado.obterMediaTempoRespostaMs()),
                    resultado.obterSeloDesempenho());
            VisaoConsole.exibirLinha(linhaTabela);

            String linhaEstimativa = String.format(Locale.US, "  -> Estimativa prox CPU: %s | Amostras: %d | Desvio CPU: %.2f",
                    Formatador.formatarPercentual(resultado.estimarProximaCpu()),
                    resultado.obterQuantidadeAmostras(),
                    resultado.obterDesvioPadraoCpu());
            VisaoConsole.exibirLinha(linhaEstimativa);
        }

        VisaoConsole.exibirSeparador();
        VisaoConsole.exibirLinha("[RESUMO]");
        VisaoConsole.exibirLinha(textoResumo);
        VisaoConsole.exibirSeparador();
    }
}
