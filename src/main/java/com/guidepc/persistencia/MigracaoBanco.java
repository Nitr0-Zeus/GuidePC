package com.guidepc.persistencia;

import java.sql.SQLException;

/**
 * Cria e atualiza o schema do banco SQLite.
 * Executa automaticamente na primeira execucao.
 */
public final class MigracaoBanco {

    private static final String SCHEMA = """
        CREATE TABLE IF NOT EXISTS testes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            tipo TEXT NOT NULL,
            nivel TEXT,
            tipo_disco TEXT,
            duracao_segundos INTEGER NOT NULL,
            inicio TEXT NOT NULL,
            fim TEXT NOT NULL,
            media_cpu REAL,
            max_cpu REAL,
            min_cpu REAL,
            desvio_cpu REAL,
            media_memoria REAL,
            max_memoria REAL,
            media_resposta_ms REAL,
            max_resposta_ms REAL,
            throughput_mbps REAL,
            iops REAL,
            selo TEXT,
            estimativa_prox REAL,
            hardware_snapshot TEXT
        );

        CREATE TABLE IF NOT EXISTS amostras (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            teste_id INTEGER NOT NULL,
            timestamp_millis INTEGER NOT NULL,
            carga_cpu REAL,
            uso_memoria REAL,
            frequencia_hz INTEGER,
            temperatura REAL,
            tempo_resposta_ms REAL,
            uso_gpu REAL,
            temperatura_gpu REAL,
            FOREIGN KEY (teste_id) REFERENCES testes(id) ON DELETE CASCADE
        );

        CREATE INDEX IF NOT EXISTS idx_testes_tipo ON testes(tipo);
        CREATE INDEX IF NOT EXISTS idx_testes_inicio ON testes(inicio);
        CREATE INDEX IF NOT EXISTS idx_amostras_teste ON amostras(teste_id);
    """;

    private MigracaoBanco() {
    }

    public static void executar() {
        try {
            ConexaoBanco banco = ConexaoBanco.obterInstancia();
            for (String stmt : SCHEMA.split(";")) {
                String trim = stmt.strip();
                if (!trim.isEmpty()) {
                    banco.executar(trim);
                }
            }
            System.out.println("[GuidePC] Banco SQLite inicializado com sucesso.");
        } catch (SQLException e) {
            System.err.println("[GuidePC] Erro ao inicializar banco: " + e.getMessage());
        }
    }
}
