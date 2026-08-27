package com.guidepc.controlador;

import com.guidepc.modelo.Amostra;
import com.guidepc.modelo.NivelEstresse;
import com.guidepc.modelo.ResultadoTesteEstresse;
import com.guidepc.servico.ServicoColetorHardware;
import com.guidepc.servico.ServicoComparacao;
import com.guidepc.servico.ServicoTesteEstresse;
import com.guidepc.utilitario.Formatador;
import com.guidepc.visao.VisaoConsole;
import com.guidepc.visao.VisaoTesteEstresseConsole;

import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ComandoTesteEstresse - Acao do menu via padrao Comando. Evita if-else no Principal.
 */
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

        Optional.ofNullable(nivelEscolhido).orElseGet(() -> {
            VisaoConsole.exibirErro("Nivel invalido, usando BAIXO como padrao");
            return NivelEstresse.BAIXO;
        });

        // Garante nivel valido sem if via Optional
        NivelEstresse nivelFinal = Optional.ofNullable(nivelEscolhido).orElse(NivelEstresse.BAIXO);

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

        switch (textoDuracao) {
            case "1", "2", "3", "4" -> {
            }
            default -> VisaoConsole.exibirAviso("Duracao invalida, usando 30s padrao");
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
        return switch (Boolean.toString(this.scannerEntrada.hasNextLine())) {
            case "true" -> this.scannerEntrada.nextLine();
            default -> "";
        };
    }
}
