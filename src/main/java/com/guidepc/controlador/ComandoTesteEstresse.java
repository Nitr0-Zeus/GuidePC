package com.guidepc.controlador;

import com.guidepc.modelo.NivelEstresse;
import com.guidepc.modelo.ResultadoTesteEstresse;
import com.guidepc.servico.ServicoComparacao;
import com.guidepc.servico.ServicoTesteEstresse;
import com.guidepc.utilitario.Formatador;
import com.guidepc.visao.VisaoConsole;
import com.guidepc.visao.VisaoTesteEstresseConsole;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/** Opcao 2 do menu: coleta nivel/duracao, executa o teste e armazena o resultado. */
public final class ComandoTesteEstresse implements Comando {

    private final ServicoTesteEstresse servicoTesteEstresse;
    private final ServicoComparacao servicoComparacao;
    private final Scanner scannerEntrada;

    public ComandoTesteEstresse(
            ServicoTesteEstresse servicoTesteEstresse,
            ServicoComparacao servicoComparacao,
            Scanner scannerEntrada
    ) {
        this.servicoTesteEstresse = servicoTesteEstresse;
        this.servicoComparacao = servicoComparacao;
        this.scannerEntrada = scannerEntrada;
    }

    @Override
    public void executar() {
        VisaoTesteEstresseConsole.exibirOpcoesNivel();
        VisaoConsole.exibirLinha("Digite 1, 2 ou 3:");
        String textoNivel = this.lerLinhaSegura().trim();

        NivelEstresse nivelEscolhido = switch (textoNivel) {
            case "1" -> NivelEstresse.NORMAL;
            case "2" -> NivelEstresse.BAIXO;
            case "3" -> NivelEstresse.ALTO;
            default -> null;
        };

        NivelEstresse nivelFinal;
        if (nivelEscolhido == null) {
            VisaoConsole.exibirErro("Nivel invalido, usando BAIXO como padrao");
            nivelFinal = NivelEstresse.BAIXO;
        } else {
            nivelFinal = nivelEscolhido;
        }

        VisaoTesteEstresseConsole.exibirOpcoesDuracao();
        VisaoConsole.exibirLinha("Digite 1 a 4:");
        String textoDuracao = this.lerLinhaSegura().trim();

        int duracaoEscolhida = switch (textoDuracao) {
            case "1" -> 15;
            case "2" -> 30;
            case "3" -> 60;
            case "4" -> 120;
            default -> 30;
        };

        if (!textoDuracao.equals("1") && !textoDuracao.equals("2") && !textoDuracao.equals("3") && !textoDuracao.equals("4")) {
            VisaoConsole.exibirAviso("Duracao invalida, usando 30s padrao");
        }

        VisaoTesteEstresseConsole.exibirInicio(nivelFinal, duracaoEscolhida);

        AtomicInteger progressoAtual = new AtomicInteger(0);

        try {
            ResultadoTesteEstresse resultado = this.servicoTesteEstresse.executarTeste(
                    nivelFinal,
                    duracaoEscolhida,
                    amostra -> {
                        int progresso = progressoAtual.get();
                        VisaoTesteEstresseConsole.exibirAmostra(amostra, progresso);
                    },
                    percentual -> {
                        progressoAtual.set(percentual);
                        VisaoTesteEstresseConsole.exibirProgresso(percentual);
                    }
            );

            this.servicoComparacao.armazenarResultado(resultado);
            String resumo = String.format(
                    "Nivel %s concluido | Media CPU %s | Pico %s | Selo %s | Estimativa prox %s",
                    resultado.obterNivelEstresse().obterRotulo(),
                    Formatador.formatarPercentual(resultado.obterMediaCpu()),
                    Formatador.formatarPercentual(resultado.obterMaximoCpu()),
                    resultado.obterSeloDesempenho(),
                    Formatador.formatarPercentual(resultado.estimarProximaCpu())
            );
            VisaoTesteEstresseConsole.exibirConclusao(resumo);
            VisaoConsole.exibirSucesso("Resultado armazenado no comparativo (opcao 3 do menu)");

        } catch (InterruptedException excecaoInterrupcao) {
            Thread.currentThread().interrupt();
            VisaoConsole.exibirErro("Teste interrompido: " + excecaoInterrupcao.getMessage());
        } catch (Exception excecao) {
            VisaoConsole.exibirErro("Falha no teste: " + excecao.getMessage());
        }
    }

    private String lerLinhaSegura() {
        if (this.scannerEntrada.hasNextLine()) {
            return this.scannerEntrada.nextLine();
        }
        return "";
    }
}
