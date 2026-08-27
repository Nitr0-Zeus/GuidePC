package com.guidepc.visao;

/**
 * Utilitario base para saida em console.
 * Centraliza cabecalho, separador e barra de progresso para manter padrao visual.
 */
public final class VisaoConsole {

    private VisaoConsole() {
    }

    public static void exibirCabecalho(String tituloPrincipal) {
        System.out.println("============================================================");
        System.out.println(" " + tituloPrincipal);
        System.out.println("============================================================");
    }

    public static void exibirSeparador() {
        System.out.println("------------------------------------------------------------");
    }

    public static void exibirLinha(String conteudoLinha) {
        System.out.println(conteudoLinha);
    }

    public static void exibirLinhaFormatada(String formato, Object... argumentos) {
        System.out.printf(formato + "%n", argumentos);
    }

    public static void exibirErro(String mensagemErro) {
        System.out.println("[ERRO] " + mensagemErro);
    }

    public static void exibirAviso(String mensagemAviso) {
        System.out.println("[AVISO] " + mensagemAviso);
    }

    public static void exibirSucesso(String mensagemSucesso) {
        System.out.println("[OK] " + mensagemSucesso);
    }

    public static void limparTelaSimples() {
        System.out.println();
        System.out.println();
    }

    /**
     * Barra ASCII no formato [####------] 40%.
     * Usa \r para sobrescrever a linha; quebra linha apenas em 100%.
     */
    public static void exibirBarraProgresso(int percentualCompleto, int tamanhoBarra) {
        int quantidadePreenchida = percentualCompleto * tamanhoBarra / 100;
        int quantidadeVazia = tamanhoBarra - quantidadePreenchida;
        String barraPreenchida = "#".repeat(quantidadePreenchida);
        String barraVazia = "-".repeat(quantidadeVazia);
        System.out.printf("\r[%s%s] %d%%", barraPreenchida, barraVazia, percentualCompleto);
        if (percentualCompleto == 100) {
            System.out.println();
        }
    }
}
