package com.guidepc.utilitario;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gera nomes de arquivo para os relatorios na pasta {@code relatorios/}.
 * Sanitiza o nome do usuario para evitar caracteres invalidos no sistema de arquivos.
 */
public final class NomeArquivo {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private NomeArquivo() {
    }

    /**
     * Gera nome tipo: relatorios/guidepc_relatorio_2026-08-27_15-30-22_Joao.pdf
     * Se o usuario for vazio, usa "usuario".
     */
    public static String gerar(String extensao, String usuarioLogado, LocalDateTime instante) {
        String usuarioSanitizado = sanitizarUsuario(usuarioLogado);
        String dataFormatada = instante.format(FORMATO_DATA);
        return String.format("relatorios/guidepc_relatorio_%s_%s.%s", dataFormatada, usuarioSanitizado, extensao);
    }

    public static String gerarCsv(String usuarioLogado, LocalDateTime instante) {
        return gerar("csv", usuarioLogado, instante);
    }

    public static String gerarPdf(String usuarioLogado, LocalDateTime instante) {
        return gerar("pdf", usuarioLogado, instante);
    }

    private static String sanitizarUsuario(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            return "usuario";
        }
        // Mantem apenas letras, numeros, underline e hifen; resto vira underline
        String sanitizado = usuario.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        // Limita tamanho para nao estourar caminho no Windows
        if (sanitizado.length() > 20) {
            sanitizado = sanitizado.substring(0, 20);
        }
        return sanitizado;
    }

    /** Obtem o usuario logado do SO (System.getProperty user.name). */
    public static String obterUsuarioLogado() {
        String usuario = System.getProperty("user.name");
        if (usuario == null || usuario.isBlank()) {
            return "usuario";
        }
        return usuario;
    }
}
