package com.guidepc;

import com.guidepc.persistencia.ConexaoBanco;
import com.guidepc.persistencia.MigracaoBanco;
import com.guidepc.web.ServidorWeb;

import java.awt.Desktop;
import java.net.InetAddress;
import java.net.URI;

/**
 * Ponto de entrada do GuidePC v3.0 — 100% Web Local.
 *
 * <p>Inicia o servidor Javalin na porta 7070 e abre o navegador automaticamente.
 * Todos os dados ficam em SQLite local (guidepc.db).</p>
 */
public class GuidePCApplication {

    private static final int PORTA_PADRAO = 7070;

    /**
     * Ponto de entrada principal. Inicializa banco, servidor e navegador nesta ordem.
     */
    public static void main(String[] args) {
        int porta = PORTA_PADRAO;

        // Parse de argumentos: --port=8080
        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                try {
                    porta = Integer.parseInt(arg.substring(7));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        System.out.println();
        System.out.println("  ██████╗ ██╗███████╗████████╗    ██████╗  █████╗╗██████╗ ██╗     ██╗████████╗");
        System.out.println("  ██╔════╝██║██╔════╝╚══██╔══╝    ██╔══██╗██╔══██║██╔══██╗██║     ██║╚══██╔══╝");
        System.out.println("  ██║     ██║█████╗     ██║       ██║  ██║███████║██████╔╝██║     ██║   ██║   ");
        System.out.println("  ██║     ██║██╔══╝     ██║       ██║  ██║██╔══██║██╔══██╗██║     ██║   ██║   ");
        System.out.println("  ╚██████╗██║██║        ██║       ██████╔╝██║  ██║██████╔╝███████╗██║   ██║   ");
        System.out.println("   ╚═════╝╚═╝╚═╝        ╚═╝       ╚═════╝ ╚═╝  ╚═╝╚═════╝ ╚══════╝╚═╝   ╚═╝   ");
        System.out.println();
        System.out.println("  GuidePC v3.0 — Monitoramento de Hardware 100% Web");
        System.out.println("  ─────────────────────────────────────────────────────");

        // 1. Inicializar banco de dados SQLite (cria arquivo guidepc.db)
        System.out.println("  [1/3] Iniciando banco de dados...");
        ConexaoBanco.iniciar("guidepc.db");
        // Executa migrações para criar/atualizar tabelas conforme necessário
        MigracaoBanco.executar();

        // 2. Iniciar servidor HTTP e WebSocket do Javalin
        System.out.println("  [2/3] Iniciando servidor web...");
        ServidorWeb.iniciar(porta);

        // 3. Abrir navegador padrão do sistema automaticamente
        System.out.println("  [3/3] Abrindo navegador...");
        // Monta a URL local e tenta abrir no navegador do sistema
        String url = "http://localhost:" + porta;
        try {
            // Verifica se o ambiente suporta Desktop (GUI disponível)
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
            // Se falhar (ex: ambiente sem GUI), o usuário acessa manualmente
        }

        System.out.println();
        System.out.println("  ✓ GuidePC rodando em: " + url);
        System.out.println("  ✓ Pressione Ctrl+C para encerrar");
        System.out.println();
    }
}
