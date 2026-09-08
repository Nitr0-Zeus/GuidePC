package com.guidepc.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gerencia a conexao com o banco SQLite.
 * Singleton thread-safe com pool simples.
 */
public final class ConexaoBanco {

    private static ConexaoBanco instancia;
    private static String caminhoBanco;
    private final String url;
    private volatile boolean pragmasConfigurados = false;

    private ConexaoBanco(String caminho) {
        this.url = "jdbc:sqlite:" + caminho;
    }

    public static synchronized void iniciar(String caminho) {
        caminhoBanco = caminho;
        instancia = new ConexaoBanco(caminho);
    }

    public static synchronized ConexaoBanco obterInstancia() {
        if (instancia == null) {
            throw new IllegalStateException("Banco nao inicializado. Chame ConexaoBanco.iniciar() primeiro.");
        }
        return instancia;
    }

    public Connection obterConexao() throws SQLException {
        Connection conn = DriverManager.getConnection(this.url + "?busy_timeout=5000");
        if (!pragmasConfigurados) {
            try (var stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA busy_timeout=5000");
                stmt.execute("PRAGMA foreign_keys=ON");
            }
            pragmasConfigurados = true;
        }
        return conn;
    }

    public void executar(String sql) throws SQLException {
        try (Connection conn = obterConexao();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
