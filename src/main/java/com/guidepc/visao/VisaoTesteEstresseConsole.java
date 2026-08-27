package com.guidepc.visao;

import com.guidepc.modelo.Amostra;
import com.guidepc.modelo.NivelEstresse;
import com.guidepc.utilitario.Formatador;

/**
 * VisaoTesteEstresseConsole - Impressao console. Tabela ASCII e barra de progresso.
 */
public final class VisaoTesteEstresseConsole {

    private VisaoTesteEstresseConsole() {
    }

    public static void exibirInicio(NivelEstresse nivelEstresse, int duracaoSegundos) {
        VisaoConsole.exibirCabecalho("TESTE DE ESTRESSE - " + nivelEstresse.obterRotulo().toUpperCase());
        VisaoConsole.exibirLinha("Descricao: " + nivelEstresse.obterDescricao());
        VisaoConsole.exibirLinha("Duracao : " + duracaoSegundos + " segundos");
        VisaoConsole.exibirLinha("Pressione CTRL+C para parada emergencial (ou feche o programa)");
        VisaoConsole.exibirSeparador();
    }

    public static void exibirAmostra(Amostra amostra, int percentualProgresso) {
        String linhaAmostra = String.format(
                "CPU %s | RAM %s | Temp %s | Resp %s | %d%%",
                Formatador.formatarPercentual(amostra.cargaCpuPercentual()),
                Formatador.formatarPercentual(amostra.usoMemoriaPercentual()),
                Formatador.formatarTemperatura(amostra.temperaturaCelsius()),
                Formatador.formatarTempoMs(amostra.tempoRespostaMs()),
                percentualProgresso
        );
        System.out.print("\r" + linhaAmostra + "   ");
    }

    public static void exibirProgresso(int percentual) {
        VisaoConsole.exibirBarraProgresso(percentual, 30);
    }

    public static void exibirConclusao(String resumo) {
        System.out.println();
        VisaoConsole.exibirSeparador();
        VisaoConsole.exibirLinha("[RESULTADO CONCLUIDO]");
        VisaoConsole.exibirLinha(resumo);
        VisaoConsole.exibirSeparador();
    }

    public static void exibirOpcoesNivel() {
        VisaoConsole.exibirLinha("Escolha o nivel de estresse:");
        for (NivelEstresse nivelEstresse : NivelEstresse.values()) {
            VisaoConsole.exibirLinha("  " + (nivelEstresse.ordinal() + 1) + " - " + nivelEstresse.obterRotulo() + " : " + nivelEstresse.obterDescricao());
        }
    }

    public static void exibirOpcoesDuracao() {
        VisaoConsole.exibirLinha("Escolha a duracao (segundos): 1-15s  2-30s  3-60s  4-120s");
    }
}
