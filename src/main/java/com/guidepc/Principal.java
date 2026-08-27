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
import java.util.Scanner;

/**
 * Ponto de entrada da aplicacao.
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>instancia os servicos (coleta, estresse, comparacao);</li>
 *   <li>monta o roteamento de opcoes do menu via padrao Comando;</li>
 *   <li>mantem o loop principal ate o usuario sair ou a entrada ser encerrada.</li>
 * </ul>
 *
 * <p>O mapa de comandos evita if/else encadeado no menu - cada opcao
 * e uma classe que implementa {@link Comando}.</p>
 */
public class Principal {

    public static void main(String[] argumentos) {
        ServicoColetorHardware servicoColetorHardware = ServicoColetorHardware.obterInstancia();
        ServicoTesteEstresse servicoTesteEstresse = new ServicoTesteEstresse();
        ServicoComparacao servicoComparacao = new ServicoComparacao();
        Scanner scannerEntrada = new Scanner(System.in);

        // Roteamento do menu: chave = opcao digitada, valor = acao correspondente.
        Map<Integer, Comando> mapaComandos = Map.of(
                1, new ComandoVisaoGeral(servicoColetorHardware),
                2, new ComandoTesteEstresse(servicoTesteEstresse, servicoComparacao, scannerEntrada),
                3, new ComandoComparativo(servicoComparacao, scannerEntrada),
                4, new ComandoSair()
        );

        VisaoConsole.exibirCabecalho("GuidePC v2.0 - MONITORAMENTO DE HARDWARE");
        VisaoConsole.exibirLinha("Sistema console 100% pt-br | OSHI 6.6.4 | Java 21");
        VisaoConsole.exibirLinha("Sem interface grafica, ideal para PCs simples");
        VisaoConsole.exibirSeparador();

        while (true) {
            exibirMenu();
            String textoDigitado = lerLinhaSegura(scannerEntrada);
            if (textoDigitado == null) {
                VisaoConsole.exibirLinha("Entrada encerrada, saindo...");
                return;
            }

            int opcaoEscolhida = converterParaInteiro(textoDigitado.trim());
            Comando comandoSelecionado = mapaComandos.getOrDefault(opcaoEscolhida, new ComandoInvalido());

            try {
                comandoSelecionado.executar();
            } catch (Exception excecao) {
                VisaoConsole.exibirErro("Erro ao executar comando: " + excecao.getMessage());
            }

            VisaoConsole.exibirLinha("Pressione ENTER para continuar...");
            String linhaContinuar = lerLinhaSegura(scannerEntrada);
            if (linhaContinuar == null) {
                VisaoConsole.exibirLinha("Entrada encerrada, saindo...");
                return;
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

    /**
     * Converte texto para inteiro; retorna -1 se nao for numero valido.
     * O valor -1 cai no ComandoInvalido via getOrDefault.
     */
    private static int converterParaInteiro(String textoOriginal) {
        if (textoOriginal == null || textoOriginal.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(textoOriginal);
        } catch (NumberFormatException excecao) {
            return -1;
        }
    }

    /**
     * Le a proxima linha do scanner de forma segura.
     * Retorna null quando a entrada foi encerrada (ex: pipe fechado / CTRL+D),
     * o que permite encerrar o programa sem excecao.
     */
    private static String lerLinhaSegura(Scanner scannerEntrada) {
        if (scannerEntrada.hasNextLine()) {
            return scannerEntrada.nextLine();
        }
        return null;
    }
}
