package com.guidepc.persistencia;

import com.guidepc.modelo.Amostra;
import com.guidepc.modelo.NivelEstresse;
import com.guidepc.modelo.ResultadoTesteDisco;
import com.guidepc.modelo.ResultadoTesteEstresse;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de testes e amostras no SQLite.
 */
public final class RepositorioResultados {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private RepositorioResultados() {
    }

    // ==================== TESTES DE ESTRESSE ====================

    public static long salvarTesteEstresse(ResultadoTesteEstresse resultado, String hardwareJson) {
        String sql = """
            INSERT INTO testes (tipo, nivel, duracao_segundos, inicio, fim,
                media_cpu, max_cpu, min_cpu, desvio_cpu,
                media_memoria, max_memoria, media_resposta_ms, max_resposta_ms,
                selo, estimativa_prox, hardware_snapshot)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = ConexaoBanco.obterInstancia().obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, "ESTRESSE");
            ps.setString(2, resultado.obterNivelEstresse().name());
            ps.setInt(3, resultado.obterDuracaoSegundos());
            ps.setString(4, formatarInstant(resultado.obterInstanteInicio()));
            ps.setString(5, formatarInstant(resultado.obterInstanteFim()));
            ps.setDouble(6, resultado.obterMediaCpu());
            ps.setDouble(7, resultado.obterMaximoCpu());
            ps.setDouble(8, resultado.obterMinimoCpu());
            ps.setDouble(9, resultado.obterDesvioPadraoCpu());
            ps.setDouble(10, resultado.obterMediaMemoria());
            ps.setDouble(11, resultado.obterMaximoMemoria());
            ps.setDouble(12, resultado.obterMediaTempoRespostaMs());
            ps.setDouble(13, resultado.obterMaximoTempoRespostaMs());
            ps.setString(14, resultado.obterSeloDesempenho());
            ps.setDouble(15, resultado.estimarProximaCpu());
            ps.setString(16, hardwareJson);

            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            long testeId = keys.next() ? keys.getLong(1) : -1;

            // Salvar amostras
            if (testeId > 0) {
                salvarAmostras(testeId, resultado.obterAmostras());
            }

            return testeId;
        } catch (SQLException e) {
            System.err.println("[GuidePC] Erro ao salvar teste: " + e.getMessage());
            return -1;
        }
    }

    // ==================== TESTES DE DISCO ====================

    public static long salvarTesteDisco(ResultadoTesteDisco resultado, String hardwareJson) {
        String sql = """
            INSERT INTO testes (tipo, tipo_disco, duracao_segundos, inicio, fim,
                throughput_mbps, iops, media_resposta_ms, selo, hardware_snapshot)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = ConexaoBanco.obterInstancia().obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, "DISCO");
            ps.setString(2, resultado.tipoTeste().name());
            ps.setInt(3, resultado.duracaoSegundos());
            ps.setString(4, formatarInstant(resultado.instanteInicio()));
            ps.setString(5, formatarInstant(resultado.instanteFim()));
            ps.setDouble(6, resultado.obterThroughputMBs());
            ps.setDouble(7, resultado.iops());
            ps.setDouble(8, resultado.tempoTotalMs());
            ps.setString(9, classificarThroughput(resultado.obterThroughputMBs()));
            ps.setString(10, hardwareJson);

            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : -1;
        } catch (SQLException e) {
            System.err.println("[GuidePC] Erro ao salvar teste de disco: " + e.getMessage());
            return -1;
        }
    }

    // ==================== AMOSTRAS ====================

    private static void salvarAmostras(long testeId, List<Amostra> amostras) throws SQLException {
        String sql = """
            INSERT INTO amostras (teste_id, timestamp_millis, carga_cpu, uso_memoria,
                frequencia_hz, temperatura, tempo_resposta_ms, uso_gpu, temperatura_gpu)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = ConexaoBanco.obterInstancia().obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Amostra a : amostras) {
                ps.setLong(1, testeId);
                ps.setLong(2, a.instanteMillis());
                ps.setDouble(3, a.cargaCpuPercentual());
                ps.setDouble(4, a.usoMemoriaPercentual());
                ps.setLong(5, a.frequenciaCpuHz());
                ps.setDouble(6, a.temperaturaCelsius());
                ps.setDouble(7, a.tempoRespostaMs());
                ps.setDouble(8, a.usoGpuPercentual());
                ps.setDouble(9, a.temperaturaGpuCelsius());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ==================== CONSULTAS ====================

    public static List<Object[]> listarTestes(String tipo, String nivel, String dataInicio, String dataFim, int pagina, int tamanhoPagina) {
        List<Object[]> resultados = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, tipo, nivel, tipo_disco, duracao_segundos, inicio, fim, media_cpu, max_cpu, selo, throughput_mbps FROM testes WHERE 1=1");

        List<Object> params = new ArrayList<>();
        if (tipo != null && !tipo.isEmpty()) {
            sql.append(" AND tipo = ?");
            params.add(tipo);
        }
        if (nivel != null && !nivel.isEmpty()) {
            sql.append(" AND nivel = ?");
            params.add(nivel);
        }
        if (dataInicio != null && !dataInicio.isEmpty()) {
            sql.append(" AND inicio >= ?");
            params.add(dataInicio);
        }
        if (dataFim != null && !dataFim.isEmpty()) {
            sql.append(" AND inicio <= ?");
            params.add(dataFim);
        }

        sql.append(" ORDER BY inicio DESC LIMIT ? OFFSET ?");
        params.add(tamanhoPagina);
        params.add((pagina - 1) * tamanhoPagina);

        try (Connection conn = ConexaoBanco.obterInstancia().obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultados.add(new Object[]{
                        rs.getLong("id"),
                        rs.getString("tipo"),
                        rs.getString("nivel"),
                        rs.getString("tipo_disco"),
                        rs.getInt("duracao_segundos"),
                        rs.getString("inicio"),
                        rs.getString("fim"),
                        rs.getDouble("media_cpu"),
                        rs.getDouble("max_cpu"),
                        rs.getString("selo"),
                        rs.getDouble("throughput_mbps")
                });
            }
        } catch (SQLException e) {
            System.err.println("[GuidePC] Erro ao listar testes: " + e.getMessage());
        }
        return resultados;
    }

    public static int contarTestes(String tipo, String nivel, String dataInicio, String dataFim) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM testes WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (tipo != null && !tipo.isEmpty()) {
            sql.append(" AND tipo = ?");
            params.add(tipo);
        }
        if (nivel != null && !nivel.isEmpty()) {
            sql.append(" AND nivel = ?");
            params.add(nivel);
        }
        if (dataInicio != null && !dataInicio.isEmpty()) {
            sql.append(" AND inicio >= ?");
            params.add(dataInicio);
        }
        if (dataFim != null && !dataFim.isEmpty()) {
            sql.append(" AND inicio <= ?");
            params.add(dataFim);
        }

        try (Connection conn = ConexaoBanco.obterInstancia().obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    public static Optional<Object[]> obterTestePorId(long id) {
        String sql = "SELECT * FROM testes WHERE id = ?";
        try (Connection conn = ConexaoBanco.obterInstancia().obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new Object[]{
                        rs.getLong("id"),
                        rs.getString("tipo"),
                        rs.getString("nivel"),
                        rs.getString("tipo_disco"),
                        rs.getInt("duracao_segundos"),
                        rs.getString("inicio"),
                        rs.getString("fim"),
                        rs.getDouble("media_cpu"),
                        rs.getDouble("max_cpu"),
                        rs.getDouble("min_cpu"),
                        rs.getDouble("desvio_cpu"),
                        rs.getDouble("media_memoria"),
                        rs.getDouble("max_memoria"),
                        rs.getDouble("media_resposta_ms"),
                        rs.getDouble("max_resposta_ms"),
                        rs.getDouble("throughput_mbps"),
                        rs.getDouble("iops"),
                        rs.getString("selo"),
                        rs.getDouble("estimativa_prox"),
                        rs.getString("hardware_snapshot")
                });
            }
        } catch (SQLException e) {
            System.err.println("[GuidePC] Erro ao obter teste: " + e.getMessage());
        }
        return Optional.empty();
    }

    public static List<Amostra> obterAmostras(long testeId) {
        List<Amostra> amostras = new ArrayList<>();
        String sql = "SELECT * FROM amostras WHERE teste_id = ? ORDER BY timestamp_millis";
        try (Connection conn = ConexaoBanco.obterInstancia().obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, testeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                amostras.add(new Amostra(
                        rs.getLong("timestamp_millis"),
                        rs.getDouble("carga_cpu"),
                        rs.getDouble("uso_memoria"),
                        rs.getLong("frequencia_hz"),
                        rs.getDouble("temperatura"),
                        rs.getDouble("tempo_resposta_ms"),
                        rs.getDouble("uso_gpu"),
                        rs.getDouble("temperatura_gpu")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[GuidePC] Erro ao obter amostras: " + e.getMessage());
        }
        return amostras;
    }

    public static boolean deletarTeste(long id) {
        String sql = "DELETE FROM testes WHERE id = ?";
        try (Connection conn = ConexaoBanco.obterInstancia().obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[GuidePC] Erro ao deletar teste: " + e.getMessage());
            return false;
        }
    }

    // ==================== UTILITARIOS ====================

    private static String formatarInstant(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(FORMATO);
    }

    private static String classificarThroughput(double mbPorSegundo) {
        if (mbPorSegundo >= 500) return "Excelente";
        if (mbPorSegundo >= 100) return "Bom";
        if (mbPorSegundo >= 50) return "Regular";
        return "Lento";
    }
}
