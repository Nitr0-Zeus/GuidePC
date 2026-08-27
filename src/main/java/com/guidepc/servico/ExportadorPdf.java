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
import com.lowagie.text.Rectangle;
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
 * Gera relatório PDF local — visual GuidePC, sem cara de IA.
 * Fica em relatorios/ e serve como backup físico para uso escolar.
 */
public final class ExportadorPdf {

    // Paleta GuidePC — espelha o console color 0C (vermelho sobre preto)
    private static final Color VERMELHO_GUIDE = new Color(139, 0, 0);
    private static final Color PRETO_SUAVE = new Color(26, 26, 26);
    private static final Color CINZA_BORDA = new Color(220, 220, 220);
    private static final Color CINZA_FUNDO = new Color(248, 248, 248);
    private static final Color AZUL_ANTIGO = new Color(45, 85, 140); // removido, mantido só se precisar

    private static final DateTimeFormatter FORMATO_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Font FONTE_FAIXA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font FONTE_FAIXA_SUB = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(255, 220, 220));
    private static final Font FONTE_MARCA = FontFactory.getFont(FontFactory.COURIER, 4, new Color(225, 200, 200));
    private static final Font FONTE_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRETO_SUAVE);
    private static final Font FONTE_SUBTITULO_LINHA = FontFactory.getFont(FontFactory.HELVETICA, 7, VERMELHO_GUIDE);
    private static final Font FONTE_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font FONTE_PEQUENA = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(110, 110, 110));
    private static final Font FONTE_CABECALHO_TABELA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font FONTE_RODAPE = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(120, 120, 120));

    private ExportadorPdf() {
    }

    public static void gerar(Map<NivelEstresse, ResultadoTesteEstresse> mapaResultados,
                             String textoResumo,
                             Path caminhoDestino,
                             InformacoesHardware informacoesHardware) throws IOException, DocumentException {

        LocalDateTime agora = LocalDateTime.now();
        String dataHoraGeracao = agora.format(FORMATO_BR);
        String usuarioLogado = System.getProperty("user.name", "usuario");
        String hostName = obterHostName();
        String janelaColeta = formatarJanelaColeta(mapaResultados);

        Document documento = new Document(PageSize.A4, 36, 36, 28, 32);
        FileOutputStream saida = new FileOutputStream(caminhoDestino.toFile());
        try {
            PdfWriter.getInstance(documento, saida);
            documento.open();

            // ----- FAIXA SUPERIOR VERMELHA COM IDENTIDADE -----
            PdfPTable faixa = new PdfPTable(2);
            faixa.setWidthPercentage(100);
            faixa.setWidths(new float[]{55, 45});
            PdfPCell celEsq = new PdfPCell(new Phrase("GuidePC", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE)));
            celEsq.setBackgroundColor(VERMELHO_GUIDE);
            celEsq.setBorder(Rectangle.NO_BORDER);
            celEsq.setPaddingLeft(8);
            celEsq.setPaddingTop(6);
            celEsq.setPaddingBottom(6);
            celEsq.setVerticalAlignment(Element.ALIGN_MIDDLE);
            PdfPCell celDir = new PdfPCell(new Phrase("Relatorio de Hardware  •  v2.1", FONTE_FAIXA_SUB));
            celDir.setBackgroundColor(VERMELHO_GUIDE);
            celDir.setBorder(Rectangle.NO_BORDER);
            celDir.setHorizontalAlignment(Element.ALIGN_RIGHT);
            celDir.setVerticalAlignment(Element.ALIGN_MIDDLE);
            celDir.setPaddingRight(8);
            faixa.addCell(celEsq);
            faixa.addCell(celDir);
            documento.add(faixa);

            // Marca d'água ASCII sutil — mesma arte do console, 4pt rosado claro
            String[] marca = {
                    "  ________      .__    .___    ___________________  ",
                    " /  _____/ __ __|__| __| _/____\\______   \\_   ___ \\ ",
                    "/   \\  ___|  |  \\  |/ __ |/ __ \\|     ___/    \\  \\/ ",
                    "\\    \\_\\  \\  |  /  / /_/ \\  ___/|    |   \\     \\____",
                    " \\______  /____/|__\\____ |\\___  >____|    \\______  /",
                    "        \\/              \\/    \\/                 \\/ "
            };
            for (String linha : marca) {
                Paragraph p = new Paragraph(linha, FONTE_MARCA);
                p.setAlignment(Element.ALIGN_CENTER);
                p.setSpacingBefore(0);
                p.setSpacingAfter(0);
                documento.add(p);
            }
            documento.add(espaco(4));

            // Linha humana única — data/hora + usuário + período (sem OSHI/Java)
            Paragraph linhaHumana = new Paragraph(
                    String.format("Emitido em %s  —  %s em %s  •  Periodo %s",
                            dataHoraGeracao, usuarioLogado, hostName, janelaColeta),
                    FONTE_PEQUENA);
            linhaHumana.setAlignment(Element.ALIGN_CENTER);
            documento.add(linhaHumana);
            documento.add(espaco(10));

            // ----- 1. MAQUINA -----
            Paragraph sec1 = new Paragraph("1. Maquina", FONTE_SUBTITULO);
            documento.add(sec1);
            // underline fino vermelho
            PdfPTable underline1 = new PdfPTable(1);
            underline1.setWidthPercentage(100);
            PdfPCell ul1 = new PdfPCell();
            ul1.setFixedHeight(1.2f);
            ul1.setBackgroundColor(VERMELHO_GUIDE);
            ul1.setBorder(Rectangle.NO_BORDER);
            underline1.addCell(ul1);
            documento.add(underline1);
            documento.add(espaco(4));

            if (informacoesHardware != null) {
                PdfPTable tabelaVisao = new PdfPTable(2);
                tabelaVisao.setWidthPercentage(100);
                tabelaVisao.setWidths(new float[]{28, 72});
                // Usa estilo minimalista: rótulo com ■ vermelho + valor, borda só horizontal
                adicionarLinhaVisao(tabelaVisao, "CPU", informacoesHardware.processador().fabricante() + " " + informacoesHardware.processador().modelo()
                        + "  •  " + informacoesHardware.processador().nucleosFisicos() + "F / " + informacoesHardware.processador().nucleosLogicos() + "T  •  " + Formatador.formatarFrequencia(informacoesHardware.processador().frequenciaMaximaHz()));
                adicionarLinhaVisao(tabelaVisao, "RAM", Formatador.formatarBytes(informacoesHardware.memoria().totalBytes())
                        + " total  •  " + Formatador.formatarBytes(informacoesHardware.memoria().disponivelBytes()) + " livre");
                adicionarLinhaVisao(tabelaVisao, "Discos", formatarDiscos(informacoesHardware));
                adicionarLinhaVisao(tabelaVisao, "GPU", formatarGpu(informacoesHardware));
                adicionarLinhaVisao(tabelaVisao, "SO", informacoesHardware.sistemaOperacional().familia() + " " + informacoesHardware.sistemaOperacional().versao()
                        + "  •  Uptime " + Formatador.formatarTempoAtividade(informacoesHardware.sistemaOperacional().tempoAtividadeSegundos()));
                documento.add(tabelaVisao);
                documento.add(espaco(10));
            }

            // ----- 2. COMPARATIVO -----
            Paragraph sec2 = new Paragraph("2. Comparativo  —  Atual vs Proximo", FONTE_SUBTITULO);
            documento.add(sec2);
            PdfPTable underline2 = new PdfPTable(1);
            underline2.setWidthPercentage(100);
            PdfPCell ul2 = new PdfPCell();
            ul2.setFixedHeight(1.2f);
            ul2.setBackgroundColor(VERMELHO_GUIDE);
            ul2.setBorder(Rectangle.NO_BORDER);
            underline2.addCell(ul2);
            documento.add(underline2);
            documento.add(espaco(4));

            if (mapaResultados.isEmpty()) {
                Paragraph aviso = new Paragraph("Nenhum teste executado ainda. Execute Normal, Baixo e Alto no menu Teste antes de gerar o relatorio.", FONTE_NORMAL);
                aviso.setAlignment(Element.ALIGN_CENTER);
                documento.add(aviso);
            } else {
                PdfPTable tabela = new PdfPTable(6);
                tabela.setWidthPercentage(100);
                tabela.setWidths(new float[]{18, 16, 16, 16, 18, 16});
                String[] cabecalhos = {"NIVEL", "MEDIA CPU", "PICO CPU", "MEDIA RAM", "RESP MEDIA", "SELO"};
                for (String cab : cabecalhos) {
                    PdfPCell celula = new PdfPCell(new Phrase(cab, FONTE_CABECALHO_TABELA));
                    celula.setBackgroundColor(PRETO_SUAVE);
                    celula.setHorizontalAlignment(Element.ALIGN_CENTER);
                    celula.setPadding(5);
                    celula.setBorderColor(PRETO_SUAVE);
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
                    // Selo em texto puro ASCII corporativo — sem pill colorido
                    tabela.addCell(celulaCentro(resultado.obterSeloDesempenho()));
                }
                documento.add(tabela);
                // Nota de projeção fora da tabela, sutil, sem mencionar IA
                Paragraph projecao = new Paragraph(construirNotaProjecao(mapaResultados), FONTE_PEQUENA);
                projecao.setAlignment(Element.ALIGN_LEFT);
                projecao.setSpacingBefore(4);
                documento.add(projecao);
                documento.add(espaco(6));
            }

            // ----- 3. RESUMO -----
            Paragraph sec3 = new Paragraph("3. Resumo", FONTE_SUBTITULO);
            documento.add(sec3);
            PdfPTable underline3 = new PdfPTable(1);
            underline3.setWidthPercentage(100);
            PdfPCell ul3 = new PdfPCell();
            ul3.setFixedHeight(1.2f);
            ul3.setBackgroundColor(VERMELHO_GUIDE);
            ul3.setBorder(Rectangle.NO_BORDER);
            underline3.addCell(ul3);
            documento.add(underline3);
            documento.add(espaco(4));
            // Filtra nota técnica duplicada do textoResumo para não expor IA
            String resumoLimpo = limparResumo(textoResumo);
            Paragraph resumo = new Paragraph(resumoLimpo != null ? resumoLimpo : "Sem resumo.", FONTE_NORMAL);
            resumo.setAlignment(Element.ALIGN_JUSTIFIED);
            resumo.setLeading(12);
            documento.add(resumo);

            // ----- RODAPE MINIMO -----
            documento.add(espaco(18));
            PdfPTable rodape = new PdfPTable(2);
            rodape.setWidthPercentage(100);
            rodape.setWidths(new float[]{70, 30});
            PdfPCell rEsq = new PdfPCell(new Phrase(String.format("GuidePC v2.1  •  %s", caminhoDestino.getFileName()), FONTE_RODAPE));
            rEsq.setBorder(Rectangle.NO_BORDER);
            rEsq.setHorizontalAlignment(Element.ALIGN_LEFT);
            PdfPCell rDir = new PdfPCell(new Phrase("Pagina 1  •  Uso local", FONTE_RODAPE));
            rDir.setBorder(Rectangle.NO_BORDER);
            rDir.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rodape.addCell(rEsq);
            rodape.addCell(rDir);
            documento.add(rodape);

        } finally {
            if (documento.isOpen()) {
                documento.close();
            } else {
                saida.close();
            }
        }
    }

    private static String construirNotaProjecao(Map<NivelEstresse, ResultadoTesteEstresse> mapa) {
        // Pega primeiro nivel com dados para exemplo; se tiver múltiplos, lista todos
        StringBuilder sb = new StringBuilder("* Projecao proxima afericao: ");
        boolean primeiro = true;
        for (NivelEstresse nivel : NivelEstresse.values()) {
            ResultadoTesteEstresse r = mapa.get(nivel);
            if (r == null) continue;
            if (!primeiro) sb.append("  •  ");
            sb.append(String.format(Locale.US, "%s %s (%d amostras, desvio %.2f)",
                    nivel.name(), Formatador.formatarPercentual(r.estimarProximaCpu()),
                    r.obterQuantidadeAmostras(), r.obterDesvioPadraoCpu()));
            primeiro = false;
        }
        if (primeiro) return "";
        return sb.toString();
    }

    private static String limparResumo(String texto) {
        if (texto == null) return null;
        // Remove nota defensiva que denuncia IA
        return texto.replace("Nota: estimativa 'proximo' e regressao linear simples, nao e predicao com inteligencia artificial.", "")
                .replace("Nota: estimativa 'próximo' é regressão linear simples, não é predição com inteligência artificial.", "")
                .trim();
    }

    private static Paragraph espaco(float pontos) {
        Paragraph p = new Paragraph(" ", FONTE_PEQUENA);
        p.setSpacingBefore(pontos / 2);
        p.setSpacingAfter(pontos / 2);
        return p;
    }

    private static void adicionarLinhaVisao(PdfPTable tabela, String rotulo, String valor) {
        PdfPCell c1 = new PdfPCell(new Phrase("■  " + rotulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, VERMELHO_GUIDE)));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setBorderWidthBottom(0.4f);
        c1.setBorderColorBottom(CINZA_BORDA);
        c1.setPadding(4);
        c1.setBackgroundColor(Color.WHITE);
        tabela.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(valor != null ? valor : "N/D", FONTE_NORMAL));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setBorderWidthBottom(0.4f);
        c2.setBorderColorBottom(CINZA_BORDA);
        c2.setPadding(4);
        tabela.addCell(c2);
    }

    private static PdfPCell celulaCentro(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, FONTE_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(4);
        c.setBorderColor(CINZA_BORDA);
        return c;
    }

    private static String obterHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "host-desconhecido";
        }
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
