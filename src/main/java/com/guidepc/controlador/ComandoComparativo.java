package com.guidepc.controlador;

import com.guidepc.servico.ServicoComparacao;
import com.guidepc.visao.VisaoComparativoConsole;
import com.guidepc.visao.VisaoConsole;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/** Opcao 3 do menu: exibe tabela comparativa e oferece exportacao em CSV. */
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

        if (this.servicoComparacao.obterTodosResultados().isEmpty()) {
            return;
        }

        VisaoConsole.exibirLinha("Deseja exportar CSV? (s/n)");
        String resposta = this.lerLinhaSegura().trim().toLowerCase();

        if (resposta.equals("s") || resposta.equals("sim")) {
            String nomeArquivo = "guidepc_relatorio.csv";
            try (FileWriter escritor = new FileWriter(nomeArquivo)) {
                escritor.write(this.servicoComparacao.exportarCsv());
                VisaoConsole.exibirSucesso("CSV exportado para: " + nomeArquivo);
            } catch (IOException excecao) {
                VisaoConsole.exibirErro("Falha ao exportar: " + excecao.getMessage());
            }
        } else {
            VisaoConsole.exibirLinha("Exportacao cancelada");
        }
    }

    private String lerLinhaSegura() {
        if (this.scannerEntrada.hasNextLine()) {
            return this.scannerEntrada.nextLine();
        }
        return "";
    }
}
