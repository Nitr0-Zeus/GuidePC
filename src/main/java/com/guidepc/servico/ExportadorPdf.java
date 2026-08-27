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
            documento.add(espaco(8));

            // ----- BOX COMO LER (antes dos números, para leigo entender) -----
            PdfPTable boxLer = new PdfPTable(1);
            boxLer.setWidthPercentage(100);
            PdfPCell celBox = new PdfPCell();
            celBox.setBackgroundColor(new Color(255, 248, 248));
            celBox.setBorderColor(new Color(224, 207, 207));
            celBox.setBorderWidth(0.6f);
            celBox.setPadding(7);
            // Título do box
            Paragraph tituloBox = new Paragraph("Como ler este relatorio", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, VERMELHO_GUIDE));
            tituloBox.setSpacingAfter(3);
            celBox.addElement(tituloBox);
            String[] bullets = {
                    "Uso do processador: esforco do cerebro do PC (0% livre, 100% no maximo) — seu sistema e o PC dividem essa carga.",
                    "Memoria: quanto da RAM esta ocupada. Windows costuma deixar alto, nao e defeito.",
                    "Pico: maior esforco em um instante isolado.",
                    "Tempo de resposta: rapidez para fazer uma conta — quanto menor, melhor.",
                    "Avaliacao: resumo simples — Otimo (ate 60% e <5ms), Atencao (ate 85% e <15ms), Critico (acima, verificar ventilacao).",
                    "Tendencia: chute do proximo teste se repetir agora, baseado nas ultimas leituras."
            };
            for (String b : bullets) {
                Paragraph p = new Paragraph("•  " + b, FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(60, 60, 60)));
                p.setLeading(9);
                p.setSpacingAfter(1);
                celBox.addElement(p);
            }
            boxLer.addCell(celBox);
            documento.add(boxLer);
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
                // Rótulos meio a meio: técnicos mas com explicação leiga
                adicionarLinhaVisao(tabelaVisao, "Processador", informacoesHardware.processador().fabricante() + " " + informacoesHardware.processador().modelo()
                        + "  •  " + informacoesHardware.processador().nucleosFisicos() + " nucleos / " + informacoesHardware.processador().nucleosLogicos() + " tarefas  •  " + Formatador.formatarFrequencia(informacoesHardware.processador().frequenciaMaximaHz()));
                adicionarLinhaVisao(tabelaVisao, "Memoria (RAM)", Formatador.formatarBytes(informacoesHardware.memoria().totalBytes())
                        + " no total  •  " + Formatador.formatarBytes(informacoesHardware.memoria().disponivelBytes()) + " livre agora");
                adicionarLinhaVisao(tabelaVisao, "Armazenamento", formatarDiscos(informacoesHardware));
                adicionarLinhaVisao(tabelaVisao, "Placa de video", formatarGpu(informacoesHardware));
                adicionarLinhaVisao(tabelaVisao, "Sistema", informacoesHardware.sistemaOperacional().familia() + " " + informacoesHardware.sistemaOperacional().versao()
                        + "  •  Ligado ha " + Formatador.formatarTempoAtividade(informacoesHardware.sistemaOperacional().tempoAtividadeSegundos()));
                documento.add(tabelaVisao);
                // Nota leiga abaixo da tabela
                Paragraph notaVisao = new Paragraph("* Dados coletados agora; nao sao dados pessoais — so hardware.", FONTE_PEQUENA);
                notaVisao.setAlignment(Element.ALIGN_LEFT);
                notaVisao.setSpacingBefore(3);
                documento.add(notaVisao);
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
                tabela.setWidths(new float[]{20, 14, 14, 14, 16, 16});
                // Cabeçalhos meio a meio: termo técnico + leigo em linha única
                String[] cabecalhos = {"Situação", "Uso médio", "Pico", "Memória", "Resposta", "Avaliação"};
                for (String cab : cabecalhos) {
                    PdfPCell celula = new PdfPCell(new Phrase(cab, FONTE_CABECALHO_TABELA));
                    celula.setBackgroundColor(PRETO_SUAVE);
                    celula.setHorizontalAlignment(Element.ALIGN_CENTER);
                    celula.setPadding(4);
                    celula.setBorderColor(PRETO_SUAVE);
                    tabela.addCell(celula);
                }
                for (NivelEstresse nivel : NivelEstresse.values()) {
                    ResultadoTesteEstresse resultado = mapaResultados.get(nivel);
                    if (resultado == null) continue;
                    // Usa rótulo leigo do nível
                    tabela.addCell(celulaCentro(nivel.obterRotulo().replace(" (base)", "")));
                    tabela.addCell(celulaCentroExplicativo(Formatador.formatarPercentual(resultado.obterMediaCpu()), interpretarUso(resultado.obterMediaCpu())));
                    tabela.addCell(celulaCentroExplicativo(Formatador.formatarPercentual(resultado.obterMaximoCpu()), interpretarPico(resultado.obterMaximoCpu())));
                    tabela.addCell(celulaCentro(Formatador.formatarPercentual(resultado.obterMediaMemoria())));
                    tabela.addCell(celulaCentroExplicativo(Formatador.formatarTempoMs(resultado.obterMediaTempoRespostaMs()), interpretarResposta(resultado.obterMediaTempoRespostaMs())));
                    // Selo em texto puro ASCII corporativo — Ótimo/Atenção/Crítico
                    tabela.addCell(celulaCentro(resultado.obterSeloDesempenho()));
                }
                documento.add(tabela);
                // Legenda leiga da avaliação + tendência
                Paragraph legenda = new Paragraph("Avaliação: Ótimo (até 60% e <5ms)  •  Atenção (até 85% e <15ms)  •  Crítico (acima, verificar ventilação)", FONTE_PEQUENA);
                legenda.setSpacingBefore(3);
                documento.add(legenda);
                Paragraph projecao = new Paragraph(construirNotaProjecao(mapaResultados), FONTE_PEQUENA);
                projecao.setAlignment(Element.ALIGN_LEFT);
                projecao.setSpacingBefore(2);
                documento.add(projecao);
                documento.add(espaco(6));
            }

            // ----- 3. RESUMO EXPLICATIVO (meio a meio, para leigo) -----
            Paragraph sec3 = new Paragraph("3. Resumo — o que isso significa", FONTE_SUBTITULO);
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
            String resumoLeigo = construirResumoLeigo(mapaResultados);
            // Fallback para texto original filtrado se não houver mapa
            if (resumoLeigo == null || resumoLeigo.isBlank()) {
                resumoLeigo = limparResumo(textoResumo);
            }
            Paragraph resumo = new Paragraph(resumoLeigo != null ? resumoLeigo : "Sem resumo.", FONTE_NORMAL);
            resumo.setAlignment(Element.ALIGN_JUSTIFIED);
            resumo.setLeading(12);
            documento.add(resumo);
            // Dica prática leiga
            Paragraph dica = new Paragraph("Dica: pico acima de 90% no teste Alto indica aquecimento — verifique ventilação e poeira. Memoria alta e constante é normal no Windows com navegador aberto.", FONTE_PEQUENA);
            dica.setSpacingBefore(6);
            documento.add(dica);

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
        StringBuilder sb = new StringBuilder("* Tendência: se repetir agora, deve ficar perto de ");
        boolean primeiro = true;
        for (NivelEstresse nivel : NivelEstresse.values()) {
            ResultadoTesteEstresse r = mapa.get(nivel);
            if (r == null) continue;
            if (!primeiro) sb.append("  •  ");
            sb.append(String.format(Locale.US, "%s %s (baseado nas últimas %d leituras)",
                    nivel.obterRotulo().replace(" (base)", ""), Formatador.formatarPercentual(r.estimarProximaCpu()),
                    Math.min(10, (int) r.obterQuantidadeAmostras())));
            primeiro = false;
        }
        if (primeiro) return "";
        return sb.toString();
    }

    private static String construirResumoLeigo(Map<NivelEstresse, ResultadoTesteEstresse> mapa) {
        if (mapa == null || mapa.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (NivelEstresse nivel : NivelEstresse.values()) {
            ResultadoTesteEstresse r = mapa.get(nivel);
            if (r == null) continue;
            String situacao = nivel.obterRotulo().replace(" (base)", "");
            String avaliacao = r.obterSeloDesempenho();
            String fraseAvaliacao = switch (avaliacao) {
                case "Ótimo" -> "tranquilo para navegar e editar textos";
                case "Atenção" -> "exigiu bastante, ainda dentro do esperado";
                default -> "exigiu muito — verifique ventilação se repetir";
            };
            sb.append(String.format(Locale.US,
                    "Em %s, seu sistema usou %.1f%% em média (pico %.1f%%) e memória %.1f%%, resposta %.1f ms — Avaliação %s, %s.\n",
                    situacao.toLowerCase(), r.obterMediaCpu(), r.obterMaximoCpu(),
                    r.obterMediaMemoria(), r.obterMediaTempoRespostaMs(), avaliacao, fraseAvaliacao));
        }
        if (mapa.size() >= 2) {
            // Delta leigo se houver Normal e Alto
            ResultadoTesteEstresse normal = mapa.get(NivelEstresse.NORMAL);
            ResultadoTesteEstresse alto = mapa.get(NivelEstresse.ALTO);
            if (normal != null && alto != null) {
                double delta = alto.obterMediaCpu() - normal.obterMediaCpu();
                sb.append(String.format(Locale.US, "\nComparando repouso e esforço máximo, o uso subiu %.1f pontos — %s",
                        delta, delta > 50 ? "seu PC alcança carga facilmente, bom para teste de estabilidade." : "variação moderada."));
            }
        }
        return sb.toString().trim();
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

    /** Célula com valor + subtítulo leigo em tamanho menor (ex: 33,3% — uso leve). */
    private static PdfPCell celulaCentroExplicativo(String valor, String explicacao) {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Phrase(valor + "\n", FONTE_NORMAL));
        p.add(new Phrase(explicacao, FontFactory.getFont(FontFactory.HELVETICA, 6, new Color(90, 90, 90))));
        PdfPCell c = new PdfPCell(p);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(3);
        c.setBorderColor(CINZA_BORDA);
        return c;
    }

    private static String interpretarUso(double percentual) {
        if (Double.isNaN(percentual)) return "";
        if (percentual < 30) return "uso leve";
        if (percentual < 60) return "uso moderado";
        if (percentual < 85) return "uso alto";
        return "uso muito alto";
    }

    private static String interpretarPico(double pico) {
        if (Double.isNaN(pico)) return "";
        if (pico < 60) return "pico tranquilo";
        if (pico < 85) return "pico alto";
        if (pico < 95) return "pico critico";
        return "pico extremo";
    }

    private static String interpretarResposta(double ms) {
        if (Double.isNaN(ms)) return "";
        if (ms < 5) return "rapido";
        if (ms < 15) return "normal";
        return "lento";
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
