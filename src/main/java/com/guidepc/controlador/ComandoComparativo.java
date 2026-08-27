package com.guidepc.controlador;

import com.guidepc.servico.ServicoComparacao;
import com.guidepc.visao.VisaoComparativoConsole;
import com.guidepc.visao.VisaoConsole;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * ComandoComparativo - Acao do menu via padrao Comando. Evita if-else no Principal.
 */
public final class ComandoComparativo implements Comando {

    private final ServicoComparacao servicoComparacao;
    private final Scanner scannerEntrada;

    public ComandoComparativo(ServicoComparacao servicoComparacao, Scanner scannerEntrada) {
        this.servicoComparacao = servicoComparacao;
        this.scannerEntrada = scannerEntrada;
    }

    @Override
    public void executar() {
        String textoResumo = this.servicoComparacao.gerarTextoResumo();
        VisaoComparativoConsole.exibir(this.servicoComparacao.obterTodosResultados(), textoResumo);

        boolean mapaVazio = this.servicoComparacao.obterTodosResultados().isEmpty();
        switch (Boolean.toString(mapaVazio)) {
            case "true" -> {
                return;
            }
            default -> {
            }
        }

        VisaoConsole.exibirLinha("Deseja exportar CSV? (s/n)");
        String resposta = this.lerLinhaSegura().trim().toLowerCase();

        switch (resposta) {
            case "s", "sim" -> {
                String nomeArquivo = "guidepc_relatorio.csv";
                try (FileWriter escritor = new FileWriter(nomeArquivo)) {
                    escritor.write(this.servicoComparacao.exportarCsv());
                    VisaoConsole.exibirSucesso("CSV exportado para: " + nomeArquivo);
                } catch (IOException excecao) {
                    VisaoConsole.exibirErro("Falha ao exportar: " + excecao.getMessage());
                }
            }
            default -> VisaoConsole.exibirLinha("Exportacao cancelada");
        }
    }

    private String lerLinhaSegura() {
        return switch (Boolean.toString(this.scannerEntrada.hasNextLine())) {
            case "true" -> this.scannerEntrada.nextLine();
            default -> "";
        };
    }
}
