package com.guidepc.web.controlador;

import com.guidepc.persistencia.RepositorioResultados;
import io.javalin.http.Context;
import io.javalin.config.RoutesConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Controller de relatórios — exportação CSV e PDF.
 *
 * Rotas registradas:
 *   GET /api/relatorio/csv → exporta todos os testes em formato CSV
 *   GET /api/relatorio/pdf → exporta todos os testes em formato PDF
 */
public final class RelatorioController {

    private RelatorioController() {
    }

    /**
     * Registra todas as rotas deste controller no Javalin.
     */
    public static void registrar(RoutesConfig routes) {
        routes.get("/api/relatorio/csv", RelatorioController::exportarCsv);
        routes.get("/api/relatorio/pdf", RelatorioController::exportarPdf);
    }

    /**
     * Exporta todos os testes do histórico em formato CSV.
     *
     * Rota: GET /api/relatorio/csv
     * Retorna: arquivo CSV para download (Content-Disposition: attachment).
     * Salva cópia em relatorios/guidepc_relatorio.csv.
     * Erro 404: não há dados para exportar.
     */
    private static void exportarCsv(Context ctx) {
        try {
            var testes = RepositorioResultados.listarTestes(null, null, null, null, 1, 10000);
            if (testes.isEmpty()) {
                ctx.status(404).json(Map.of("erro", "Sem dados para exportar"));
                return;
            }

            StringBuilder csv = new StringBuilder();
            csv.append("ID,Tipo,Nivel,Disco,Duracao(s),Inicio,Fim,CPU Media,CPU Max,Selo,Throughput(MB/s)\n");
            for (Object[] t : testes) {
                csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    t[0], escapeCsv(t[1]), escapeCsv(t[2]), escapeCsv(t[3]), t[4],
                    escapeCsv(t[5]), escapeCsv(t[6]),
                    t[7], t[8], escapeCsv(t[9]), t[10]));
            }

            Path dir = Path.of("relatorios");
            Files.createDirectories(dir);
            Path arquivo = dir.resolve("guidepc_relatorio.csv");
            Files.writeString(arquivo, csv.toString());

            ctx.header("Content-Disposition", "attachment; filename=guidepc_relatorio.csv");
            ctx.contentType("text/csv");
            ctx.result(Files.readString(arquivo));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Exporta todos os testes do histórico em formato PDF.
     *
     * Rota: GET /api/relatorio/pdf
     * Retorna: arquivo PDF para download (Content-Disposition: attachment).
     * Utiliza a biblioteca OpenPDF para gerar o documento.
     * Salva cópia em relatorios/guidepc_relatorio.pdf.
     * Erro 404: não há dados para exportar.
     */
    private static void exportarPdf(Context ctx) {
        try {
            var testes = RepositorioResultados.listarTestes(null, null, null, null, 1, 10000);
            if (testes.isEmpty()) {
                ctx.status(404).json(Map.of("erro", "Sem dados para exportar"));
                return;
            }

            // Gerar PDF simples usando OpenPDF
            Path dir = Path.of("relatorios");
            Files.createDirectories(dir);
            Path arquivo = dir.resolve("guidepc_relatorio.pdf");

            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            try (var outputStream = Files.newOutputStream(arquivo)) {
                com.lowagie.text.pdf.PdfWriter.getInstance(doc, outputStream);
                doc.open();

                doc.add(new com.lowagie.text.Paragraph("GuidePC - Relatório de Testes"));
                doc.add(new com.lowagie.text.Paragraph(" "));
                doc.add(new com.lowagie.text.Paragraph("Total de testes: " + testes.size()));
                doc.add(new com.lowagie.text.Paragraph(" "));

                for (Object[] t : testes) {
                    doc.add(new com.lowagie.text.Paragraph(String.format(
                        "ID: %s | Tipo: %s | Nível: %s | Duração: %ss | CPU Média: %s%% | Selo: %s",
                        t[0], t[1], nvl(t[2]), t[4], t[7], nvl(t[9])
                    )));
                }

                doc.close();
            }

            ctx.header("Content-Disposition", "attachment; filename=guidepc_relatorio.pdf");
            ctx.contentType("application/pdf");
            ctx.result(Files.readAllBytes(arquivo));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Método auxiliar que retorna string vazia se o objeto for nulo.
     */
    private static String nvl(Object o) {
        return o != null ? String.valueOf(o) : "";
    }

    /**
     * Escapa valores para CSV, prevenindo CSV injection e quebras de formato.
     */
    private static String escapeCsv(Object valor) {
        if (valor == null) return "";
        String str = String.valueOf(valor);
        if (str.isEmpty()) return "";
        // Previne CSV injection (valores iniciando com =, +, -, @)
        if (str.charAt(0) == '=' || str.charAt(0) == '+' || str.charAt(0) == '-' || str.charAt(0) == '@') {
            str = "'" + str;
        }
        // Escapa aspas duplas e envolve em aspas se contiver virgula ou quebra de linha
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            str = "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}
