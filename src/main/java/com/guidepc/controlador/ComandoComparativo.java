package com.guidepc.controlador;

import com.guidepc.modelo.InformacoesHardware;
import com.guidepc.servico.ExportadorCsv;
import com.guidepc.servico.ExportadorPdf;
import com.guidepc.servico.ServicoColetorHardware;
import com.guidepc.servico.ServicoComparacao;
import com.guidepc.utilitario.NomeArquivo;
import com.guidepc.visao.VisaoComparativoConsole;
import com.guidepc.visao.VisaoConsole;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Scanner;

/**
 * Opção 3 do menu: exibe tabela comparativa no console e oferece
 * exportação como backup físico em relatorios/ (CSV, PDF ou ambos).
 *
 * <p>O console sempre mostra os dados; a exportação é opt-in.
 * Cada arquivo inclui data/hora da geração e usuário logado.</p>
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

        if (this.servicoComparacao.obterTodosResultados().isEmpty()) {
            return;
        }

        VisaoConsole.exibirLinha("");
        VisaoConsole.exibirLinha("Exportar relatório (backup físico em relatorios/):");
        VisaoConsole.exibirLinha("  [1] CSV   [2] PDF   [3] Ambos   [4] Cancelar");
        System.out.print("Escolha (1-4) [3]: ");
        String escolha = this.lerLinhaSegura().trim();
        if (escolha.isEmpty()) {
            escolha = "3";
        }

        boolean exportarCsv = escolha.equals("1") || escolha.equals("3");
        boolean exportarPdf = escolha.equals("2") || escolha.equals("3");

        if (!exportarCsv && !exportarPdf) {
            if (escolha.equals("4")) {
                VisaoConsole.exibirLinha("Exportação cancelada.");
            } else {
                VisaoConsole.exibirAviso("Opção inválida, nada exportado.");
            }
            return;
        }

        // Garante pasta relatorios/ na raiz do projeto
        Path pastaRelatorios = Path.of("relatorios");
        try {
            Files.createDirectories(pastaRelatorios);
        } catch (IOException e) {
            VisaoConsole.exibirErro("Não foi possível criar pasta relatorios/: " + e.getMessage());
            return;
        }

        String usuario = NomeArquivo.obterUsuarioLogado();
        LocalDateTime agora = LocalDateTime.now();

        if (exportarCsv) {
            String nomeCsv = NomeArquivo.gerarCsv(usuario, agora);
            Path caminhoCsv = Path.of(nomeCsv);
            try (FileWriter escritor = new FileWriter(caminhoCsv.toFile())) {
                escritor.write(ExportadorCsv.gerar(this.servicoComparacao.obterTodosResultados()));
                VisaoConsole.exibirSucesso("CSV salvo em: " + caminhoCsv + " (" + caminhoCsv.toFile().length() + " bytes)");
            } catch (IOException excecao) {
                VisaoConsole.exibirErro("Falha ao exportar CSV: " + excecao.getMessage());
            }
        }

        if (exportarPdf) {
            String nomePdf = NomeArquivo.gerarPdf(usuario, agora);
            Path caminhoPdf = Path.of(nomePdf);
            try {
                // Coleta dados da máquina para o cabeçalho do PDF (só neste momento, para não atrasar o teste)
                InformacoesHardware hardware = null;
                try {
                    hardware = ServicoColetorHardware.obterInstancia().coletarTudo();
                } catch (Exception ignored) {
                    // Se falhar, gera PDF apenas com os resultados dos testes
                }
                ExportadorPdf.gerar(this.servicoComparacao.obterTodosResultados(), textoResumo, caminhoPdf, hardware);
                VisaoConsole.exibirSucesso("PDF salvo em: " + caminhoPdf + " (" + caminhoPdf.toFile().length() + " bytes)");
                VisaoConsole.exibirLinha("Backup físico pronto para imprimir/entregar.");
            } catch (Exception excecao) {
                VisaoConsole.exibirErro("Falha ao exportar PDF: " + excecao.getMessage());
            }
        }
    }

    private String lerLinhaSegura() {
        if (this.scannerEntrada.hasNextLine()) {
            return this.scannerEntrada.nextLine();
        }
        return "";
    }
}
