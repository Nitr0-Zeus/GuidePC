package com.guidepc.servico;

import com.guidepc.modelo.InformacoesHardware;
import com.guidepc.modelo.NivelEstresse;
import com.guidepc.modelo.ResultadoTesteEstresse;
import com.guidepc.utilitario.Formatador;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Gera relatorio PDF local como backup fisico.
 * Fica em relatorios/ e contem: metadados de coleta (data/hora + usuario),
 * visao geral resumida da maquina e tabela comparativa.
 *
 * <p>Usa OpenPDF (fork LGPL do iText) para manter o projeto console e leve.</p>
 */
public final class ExportadorPdf {

    private static final DateTimeFormatter FORMATO_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Font FONTE_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(30, 60, 110));
    private static final Font FONTE_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(50, 50, 50));
    private static final Font FONTE_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font FONTE_PEQUENA = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(100, 100, 100));
    private static final Font FONTE_CABECALHO_TABELA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

    private ExportadorPdf() {
    }

    /**
     * Gera o PDF em disco.
     *
     * @param mapaResultados resultados por nivel (pode estar parcial)
     * @param textoResumo    resumo textual do ServicoComparacao
     * @param caminhoDestino caminho completo em relatorios/
     * @param informacoesHardware dados da maquina para o cabeçalho (pode ser null)
     */
    public static void gerar(Map<NivelEstresse, ResultadoTesteEstresse> mapaResultados,
                             String textoResumo,
                             Path caminhoDestino,
                             InformacoesHardware informacoesHardware) throws IOException, DocumentException {

        // Metadados de coleta: data/hora atual + usuario logado + hostname
        LocalDateTime agora = LocalDateTime.now();
        String dataHoraGeracao = agora.format(FORMATO_BR);
        String usuarioLogado = System.getProperty("user.name", "usuario");
        String hostName = obterHostName();

        Document documento = new Document(PageSize.A4, 36, 36, 36, 36);
        try (FileOutputStream saida = new FileOutputStream(caminhoDestino.toFile())) {
            PdfWriter.getInstance(documento, saida);
            documento.open();

            // ----- CABECALHO -----
            Paragraph titulo = new Paragraph("GuidePC v2.1 — Relatório de Hardware", FONTE_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);

            Paragraph subtitulo = new Paragraph("Monitoramento e teste de estresse — backup físico local", FONTE_PEQUENA);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(subtitulo);
            documento.add(espaco(8));

            // Metadados em tabela de 2 colunas para ficar legivel impresso
            PdfPTable tabelaMetadados = new PdfPTable(2);
            tabelaMetadados.setWidthPercentage(100);
            tabelaMetadados.setWidths(new float[]{30, 70});
            adicionarLinhaMetadado(tabelaMetadados, "Data/hora da geração:", dataHoraGeracao);
            adicionarLinhaMetadado(tabelaMetadados, "Usuário logado:", usuarioLogado + " @ " + hostName);
            adicionarLinhaMetadado(tabelaMetadados, "Sistema:", formatarSistema(informacoesHardware));
            adicionarLinhaMetadado(tabelaMetadados, "Processador:", formatarProcessador(informacoesHardware));

            // Janela de coleta (inicio/fim dos testes)
            String janelaColeta = formatarJanelaColeta(mapaResultados);
            adicionarLinhaMetadado(tabelaMetadados, "Janela de coleta:", janelaColeta);
            documento.add(tabelaMetadados);
            documento.add(espaco(12));

            // ----- VISAO GERAL RESUMIDA -----
            if (informacoesHardware != null) {
                documento.add(new Paragraph("1. Visão geral da máquina", FONTE_SUBTITULO));
                documento.add(espaco(4));
                PdfPTable tabelaVisao = new PdfPTable(2);
                tabelaVisao.setWidthPercentage(100);
                tabelaVisao.setWidths(new float[]{30, 70});
                adicionarLinhaMetadado(tabelaVisao, "CPU:", informacoesHardware.processador().fabricante() + " " + informacoesHardware.processador().modelo()
                        + " (" + informacoesHardware.processador().nucleosFisicos() + "F/" + informacoesHardware.processador().nucleosLogicos() + "T)");
                adicionarLinhaMetadado(tabelaVisao, "RAM:", Formatador.formatarBytes(informacoesHardware.memoria().totalBytes())
                        + " total, " + Formatador.formatarBytes(informacoesHardware.memoria().disponivelBytes()) + " livre");
                adicionarLinhaMetadado(tabelaVisao, "Discos:", formatarDiscos(informacoesHardware));
                adicionarLinhaMetadado(tabelaVisao, "GPU:", formatarGpu(informacoesHardware));
                adicionarLinhaMetadado(tabelaVisao, "SO:", informacoesHardware.sistemaOperacional().familia() + " " + informacoesHardware.sistemaOperacional().versao()
                        + " | Uptime " + Formatador.formatarTempoAtividade(informacoesHardware.sistemaOperacional().tempoAtividadeSegundos()));
                documento.add(tabelaVisao);
                documento.add(espaco(12));
            }

            // ----- TABELA COMPARATIVA -----
            documento.add(new Paragraph("2. Comparativo — Atual vs Próximo (estimativa)", FONTE_SUBTITULO));
            documento.add(espaco(4));

            if (mapaResultados.isEmpty()) {
                Paragraph aviso = new Paragraph("Nenhum teste executado ainda. Execute Normal, Baixo e Alto no menu Teste antes de gerar o relatório.", FONTE_NORMAL);
                aviso.setAlignment(Element.ALIGN_CENTER);
                documento.add(aviso);
            } else {
                PdfPTable tabela = new PdfPTable(6);
                tabela.setWidthPercentage(100);
                tabela.setWidths(new float[]{18, 16, 16, 16, 18, 16});
                String[] cabecalhos = {"NIVEL", "MEDIA CPU", "PICO CPU", "MEDIA RAM", "RESP MEDIA", "SELO"};
                for (String cab : cabecalhos) {
                    PdfPCell celula = new PdfPCell(new Phrase(cab, FONTE_CABECALHO_TABELA));
                    celula.setBackgroundColor(new Color(45, 85, 140));
                    celula.setHorizontalAlignment(Element.ALIGN_CENTER);
                    celula.setPadding(5);
                    tabela.addCell(celula);
                }
                for (NivelEstresse nivel : NivelEstresse.values()) {
                    ResultadoTesteEstresse resultado = mapaResultados.get(nivel);
                    if (resultado == null) continue;
                    tabela.addCell(celulaCentro(nivel.name()));
                    tabela.addCell(celulaCentro(Formatador.formatarPercentual(resultado.obterMediaCpu())));
                    tabela.addCell(celulaCentro(Formatador.formatarPercentual(resultado.obterMaximoCpu())));
                    tabela.addCell(celulaCentro(Formatador.formatarPercentual(resultado.obterMediaMemoria())));
                    tabela.addCell(celulaCentro(Formatador.formatarTempoMs(resultado.obterMediaTempoRespostaMs())));
                    tabela.addCell(celulaCentro(resultado.obterSeloDesempenho()));
                    // Linha de estimativa mesclada (colspan 6)
                    PdfPCell estimativa = new PdfPCell(new Phrase(String.format(Locale.US,
                            "Estimativa prox CPU: %s | Amostras: %d | Desvio CPU: %.2f | Duracao: %ds",
                            Formatador.formatarPercentual(resultado.estimarProximaCpu()),
                            resultado.obterQuantidadeAmostras(),
                            resultado.obterDesvioPadraoCpu(),
                            resultado.obterDuracaoSegundos()), FONTE_PEQUENA));
                    estimativa.setColspan(6);
                    estimativa.setBackgroundColor(new Color(240, 244, 250));
                    estimativa.setPadding(4);
                    tabela.addCell(estimativa);
                }
                documento.add(tabela);
                documento.add(espaco(8));
            }

            documento.add(espaco(8));
            documento.add(new Paragraph("3. Resumo", FONTE_SUBTITULO));
            documento.add(espaco(4));
            Paragraph resumo = new Paragraph(textoResumo != null ? textoResumo : "Sem resumo.", FONTE_NORMAL);
            resumo.setAlignment(Element.ALIGN_JUSTIFIED);
            documento.add(resumo);

            documento.add(espaco(16));
            Paragraph rodape = new Paragraph(
                    String.format("Gerado localmente por GuidePC v2.1 console | OSHI 6.6.4 | Java 21 | Arquivo: %s", caminhoDestino.getFileName()),
                    FONTE_PEQUENA);
            rodape.setAlignment(Element.ALIGN_CENTER);
            documento.add(rodape);

            Paragraph nota = new Paragraph("Nota: estimativa 'próximo' é regressão linear simples, não é predição com inteligência artificial.", FONTE_PEQUENA);
            nota.setAlignment(Element.ALIGN_CENTER);
            documento.add(nota);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
    }

    private static Paragraph espaco(float pontos) {
        Paragraph p = new Paragraph(" ", FONTE_PEQUENA);
        p.setSpacingBefore(pontos / 2);
        p.setSpacingAfter(pontos / 2);
        return p;
    }

    private static void adicionarLinhaMetadado(PdfPTable tabela, String rotulo, String valor) {
        PdfPCell c1 = new PdfPCell(new Phrase(rotulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK)));
        c1.setBorderColor(new Color(220, 220, 220));
        c1.setPadding(4);
        c1.setBackgroundColor(new Color(248, 248, 248));
        tabela.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(valor != null ? valor : "N/D", FONTE_NORMAL));
        c2.setBorderColor(new Color(220, 220, 220));
        c2.setPadding(4);
        tabela.addCell(c2);
    }

    private static PdfPCell celulaCentro(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, FONTE_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(4);
        return c;
    }

    private static PdfPCell celulaCentral(String texto) {
        return celulaCentro(texto);
    }

    private static String obterHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "host-desconhecido";
        }
    }

    private static String formatarSistema(InformacoesHardware hw) {
        if (hw == null) return "N/D";
        return hw.sistemaOperacional().familia() + " " + hw.sistemaOperacional().versao() + " (" + hw.sistemaOperacional().arquitetura() + ")";
    }

    private static String formatarProcessador(InformacoesHardware hw) {
        if (hw == null) return "N/D";
        return hw.processador().modelo() + " | " + hw.processador().nucleosLogicos() + " threads | " + Formatador.formatarFrequencia(hw.processador().frequenciaMaximaHz());
    }

    private static String formatarDiscos(InformacoesHardware hw) {
        if (hw == null || hw.discos().isEmpty()) return "N/D";
        return hw.discos().size() + " disco(s): " + hw.discos().get(0).modelo() + (hw.discos().size() > 1 ? " +" + (hw.discos().size() - 1) : "");
    }

    private static String formatarGpu(InformacoesHardware hw) {
        if (hw == null || hw.nomesGpu().isEmpty()) return "N/D";
        return String.join(", ", hw.nomesGpu());
    }

    private static String formatarJanelaColeta(Map<NivelEstresse, ResultadoTesteEstresse> mapa) {
        if (mapa.isEmpty()) return "Sem coleta";
        // Pega o mais antigo e o mais recente
        ResultadoTesteEstresse primeiro = null, ultimo = null;
        for (ResultadoTesteEstresse r : mapa.values()) {
            if (primeiro == null || r.obterInstanteInicio().isBefore(primeiro.obterInstanteInicio())) primeiro = r;
            if (ultimo == null || r.obterInstanteFim().isAfter(ultimo.obterInstanteFim())) ultimo = r;
        }
        if (primeiro == null || ultimo == null) return "N/D";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss").withZone(java.time.ZoneId.systemDefault());
        return fmt.format(primeiro.obterInstanteInicio()) + " → " + fmt.format(ultimo.obterInstanteFim());
    }
}
