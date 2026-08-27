package com.guidepc;

import com.guidepc.controlador.Comando;
import com.guidepc.controlador.ComandoComparativo;
import com.guidepc.controlador.ComandoInvalido;
import com.guidepc.controlador.ComandoSair;
import com.guidepc.controlador.ComandoTesteEstresse;
import com.guidepc.controlador.ComandoVisaoGeral;
import com.guidepc.servico.ServicoColetorHardware;
import com.guidepc.servico.ServicoComparacao;
import com.guidepc.servico.ServicoTesteEstresse;
import com.guidepc.visao.VisaoConsole;

import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * Principal - Ponto de entrada. Loop do menu e despacho de comandos. Adicionar opcoes aqui.
 */
public class Principal {

    public static void main(String[] argumentos) {
        ServicoColetorHardware servicoColetorHardware = ServicoColetorHardware.obterInstancia();
        ServicoTesteEstresse servicoTesteEstresse = new ServicoTesteEstresse();
        ServicoComparacao servicoComparacao = new ServicoComparacao();
        Scanner scannerEntrada = new Scanner(System.in);

        // Mapa de comandos: elimina if-else via polimorfismo
        Map<Integer, Comando> mapaComandos = Map.of(
                1, new ComandoVisaoGeral(servicoColetorHardware),
                2, new ComandoTesteEstresse(servicoTesteEstresse, servicoComparacao, scannerEntrada),
                3, new ComandoComparativo(servicoComparacao, scannerEntrada),
                4, new ComandoSair()
        );

        VisaoConsole.exibirCabecalho("GuidePC v1.0.0 - MONITORAMENTO DE HARDWARE");
        VisaoConsole.exibirLinha("Sistema console 100% pt-br | OSHI 6.6.4 | Java 21");
        VisaoConsole.exibirLinha("Sem interface grafica, ideal para PCs simples");
        VisaoConsole.exibirSeparador();

        while (true) {
            exibirMenu();
            String textoDigitado = lerLinhaSegura(scannerEntrada).trim();

            int opcaoEscolhida = converterParaInteiro(textoDigitado);
            Comando comandoSelecionado = mapaComandos.getOrDefault(opcaoEscolhida, new ComandoInvalido());

            try {
                comandoSelecionado.executar();
            } catch (Exception excecao) {
                VisaoConsole.exibirErro("Erro ao executar comando: " + excecao.getMessage());
            }

            VisaoConsole.exibirLinha("Pressione ENTER para continuar...");
            String linhaContinuar = lerLinhaSegura(scannerEntrada);
            // Usa switch para tratar EOF sem if
            switch (Boolean.toString(linhaContinuar == null)) {
                case "true" -> {
                    VisaoConsole.exibirLinha("Entrada encerrada, saindo...");
                    return;
                }
                default -> {
                }
            }
        }
    }

    private static void exibirMenu() {
        VisaoConsole.exibirLinha("");
        VisaoConsole.exibirCabecalho("MENU PRINCIPAL");
        VisaoConsole.exibirLinha("[1] Visao Geral - Exibir hardware (CPU, RAM, Disco, Placa, GPU)");
        VisaoConsole.exibirLinha("[2] Teste de Estresse - Normal / Baixo / Alto (15s a 120s)");
        VisaoConsole.exibirLinha("[3] Comparativo - Atual vs Proximo + exportar CSV");
        VisaoConsole.exibirLinha("[4] Sair");
        VisaoConsole.exibirSeparador();
        System.out.print("Escolha uma opcao (1-4): ");
    }

    private static int converterParaInteiro(String textoOriginal) {
        return Optional.ofNullable(textoOriginal)
                .map(texto -> {
                    try {
                        return Integer.parseInt(texto);
                    } catch (NumberFormatException excecaoFormato) {
                        return -1;
                    }
                })
                .orElse(-1);
    }

    private static String lerLinhaSegura(Scanner scannerEntrada) {
        return switch (Boolean.toString(scannerEntrada.hasNextLine())) {
            case "true" -> scannerEntrada.nextLine();
            default -> null;
        };
    }
}
