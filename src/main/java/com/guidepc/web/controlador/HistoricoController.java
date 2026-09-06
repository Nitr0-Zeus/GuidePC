package com.guidepc.web.controlador;

import com.guidepc.persistencia.RepositorioResultados;
import io.javalin.http.Context;
import io.javalin.config.RoutesConfig;

import java.util.*;

/**
 * Controller de histórico — consultar e excluir testes salvos.
 *
 * Rotas registradas:
 *   GET    /api/historico       → lista testes com filtros e paginação
 *   DELETE /api/historico/{id}  → exclui um teste pelo ID
 */
public final class HistoricoController {

    private HistoricoController() {
    }

    /**
     * Registra todas as rotas deste controller no Javalin.
     */
    public static void registrar(RoutesConfig routes) {
        routes.get("/api/historico", HistoricoController::listar);
        routes.delete("/api/historico/{id}", HistoricoController::deletar);
    }

    /**
     * Lista testes salvos no histórico com suporte a filtros e paginação.
     *
     * Rota: GET /api/historico
     * Query params: tipo, nivel, dataInicio, dataFim, pagina (default 1),
     *               tamanhoPagina (default 15).
     * Retorna: JSON com "testes" (lista), "total", "pagina" e "totalPaginas".
     */
    @SuppressWarnings("unchecked")
    private static void listar(Context ctx) {
        try {
            String tipoFiltro = ctx.queryParam("tipo");
            String nivelFiltro = ctx.queryParam("nivel");
            String dataInicio = ctx.queryParam("dataInicio");
            String dataFim = ctx.queryParam("dataFim");
            int pagina = parseIntParam(ctx, "pagina", 1);
            int tamanhoPagina = parseIntParam(ctx, "tamanhoPagina", 15);

            List<Object[]> testes = RepositorioResultados.listarTestes(tipoFiltro, nivelFiltro, dataInicio, dataFim, pagina, tamanhoPagina);
            int total = RepositorioResultados.contarTestes(tipoFiltro, nivelFiltro, dataInicio, dataFim);
            int totalPaginas = (int) Math.ceil((double) total / tamanhoPagina);

            // Converter Object[] para lista de maps para JSON
            List<Map<String, Object>> testesConvertidos = new ArrayList<>();
            for (Object[] t : testes) {
                Map<String, Object> mapa = new LinkedHashMap<>();
                mapa.put("id", t[0]);
                mapa.put("tipo", t[1]);
                mapa.put("nivel", t[2]);
                mapa.put("tipoDisco", t[3]);
                mapa.put("duracaoSegundos", t[4]);
                mapa.put("inicio", t[5]);
                mapa.put("fim", t[6]);
                mapa.put("mediaCpu", t[7]);
                mapa.put("maxCpu", t[8]);
                mapa.put("selo", t[9]);
                mapa.put("throughputMbps", t[10]);
                testesConvertidos.add(mapa);
            }

            Map<String, Object> resposta = new LinkedHashMap<>();
            resposta.put("testes", testesConvertidos);
            resposta.put("total", total);
            resposta.put("pagina", pagina);
            resposta.put("totalPaginas", totalPaginas);

            ctx.json(resposta);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Exclui um teste do histórico pelo ID.
     *
     * Rota: DELETE /api/historico/{id}
     * Retorna: JSON com status "removido" ou erro 404 se não encontrado.
     */
    private static void deletar(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            boolean removido = RepositorioResultados.deletarTeste(id);
            if (removido) {
                ctx.json(Map.of("status", "removido"));
            } else {
                ctx.status(404).json(Map.of("erro", "Teste não encontrado"));
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Método auxiliar para parsear parâmetros de query string como int.
     * Retorna o valor padrão se o parâmetro não existir ou não for numérico.
     */
    private static int parseIntParam(Context ctx, String nome, int padrao) {
        try {
            String valor = ctx.queryParam(nome);
            return valor != null ? Integer.parseInt(valor) : padrao;
        } catch (NumberFormatException e) {
            return padrao;
        }
    }
}
