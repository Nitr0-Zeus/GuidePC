package com.guidepc.web.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guidepc.servico.GerenciadorTestes;
import com.guidepc.servico.ServicoMonitoramento;
import io.javalin.http.Context;
import io.javalin.config.RoutesConfig;

import java.util.Map;

/**
 * Controller de testes — iniciar/parar testes de estresse e disco.
 *
 * Rotas registradas:
 *   POST /api/teste/estresse → inicia teste de estresse na CPU
 *   POST /api/teste/disco     → inicia teste de disco (leitura/escrita)
 *   POST /api/teste/parar     → para teste em execução
 *   GET  /api/teste/status    → verifica se há teste em execução
 *   GET  /api/teste/alertas   → obtém configuração de alertas
 *   PUT  /api/teste/alertas   → atualiza configuração de alertas
 */
public final class TesteController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TesteController() {
    }

    /**
     * Registra todas as rotas deste controller no Javalin.
     */
    public static void registrar(RoutesConfig routes) {
        routes.post("/api/teste/estresse", TesteController::iniciarEstresse);
        routes.post("/api/teste/disco", TesteController::iniciarDisco);
        routes.post("/api/teste/parar", TesteController::parar);
        routes.get("/api/teste/status", TesteController::status);
        routes.get("/api/teste/alertas", TesteController::obterAlertas);
        routes.put("/api/teste/alertas", TesteController::atualizarAlertas);
    }

    /**
     * Inicia teste de estresse na CPU.
     *
     * Rota: POST /api/teste/estresse
     * Body JSON: { "nivel": "BAIXO|MEDIO|ALTO", "duracao": 30 }
     * Retorna: JSON com status "iniciado" e tipo "ESTRESSE".
     * Erro 409: já existe um teste em execução.
     */
    private static void iniciarEstresse(Context ctx) {
        try {
            String bodyStr = ctx.body();
            if (bodyStr == null || bodyStr.isBlank()) {
                ctx.status(400).json(Map.of("erro", "Corpo da requisicao invalido"));
                return;
            }
            ObjectNode body = MAPPER.readTree(bodyStr).deepCopy();
            String nivel = body.path("nivel").asText("BAIXO");
            int duracao = body.path("duracao").asInt(30);

            if (duracao < 5 || duracao > 600) {
                ctx.status(400).json(Map.of("erro", "Duracao deve ser entre 5 e 600 segundos"));
                return;
            }

            GerenciadorTestes.obterInstancia().iniciarEstresse(nivel, duracao);
            ctx.json(Map.of("status", "iniciado", "tipo", "ESTRESSE"));
        } catch (IllegalStateException e) {
            ctx.status(409).json(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Inicia teste de performance de disco.
     *
     * Rota: POST /api/teste/disco
     * Body JSON: { "tipo": "ESCRITA_SEQUENCIAL|LEITURA_SEQUENCIAL", "duracao": 30 }
     * Retorna: JSON com status "iniciado" e tipo "DISCO".
     * Erro 409: já existe um teste em execução.
     */
    private static void iniciarDisco(Context ctx) {
        try {
            String bodyStr = ctx.body();
            if (bodyStr == null || bodyStr.isBlank()) {
                ctx.status(400).json(Map.of("erro", "Corpo da requisicao invalido"));
                return;
            }
            ObjectNode body = MAPPER.readTree(bodyStr).deepCopy();
            String tipo = body.path("tipo").asText("ESCRITA_SEQUENCIAL");
            int duracao = body.path("duracao").asInt(30);

            if (duracao < 5 || duracao > 120) {
                ctx.status(400).json(Map.of("erro", "Duracao deve ser entre 5 e 120 segundos"));
                return;
            }

            GerenciadorTestes.obterInstancia().iniciarDisco(tipo, duracao);
            ctx.json(Map.of("status", "iniciado", "tipo", "DISCO"));
        } catch (IllegalStateException e) {
            ctx.status(409).json(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Para o teste que está em execução.
     *
     * Rota: POST /api/teste/parar
     * Retorna: JSON com status "parado".
     */
    private static void parar(Context ctx) {
        GerenciadorTestes.obterInstancia().parar();
        ctx.json(Map.of("status", "parado"));
    }

    /**
     * Verifica se há algum teste em execução.
     *
     * Rota: GET /api/teste/status
     * Retorna: JSON com campo "emExecucao" (boolean).
     */
    private static void status(Context ctx) {
        ctx.json(Map.of("emExecucao", GerenciadorTestes.obterInstancia().estaEmExecucao()));
    }

    /**
     * Obtém a configuração atual de alertas do monitoramento.
     *
     * Rota: GET /api/teste/alertas
     * Retorna: JSON com habilitado, limiteCpu, limiteTemperatura e limiteMemoria.
     */
    private static void obterAlertas(Context ctx) {
        var config = ServicoMonitoramento.obterInstancia().getConfiguracaoAlerta();
        Map<String, Object> resultado = Map.of(
            "habilitado", config.estaHabilitado(),
            "limiteCpu", config.obterLimiteCpu(),
            "limiteTemperatura", config.obterLimiteTemperatura(),
            "limiteMemoria", config.obterLimiteMemoria()
        );
        ctx.json(resultado);
    }

    /**
     * Atualiza a configuração de alertas do monitoramento.
     *
     * Rota: PUT /api/teste/alertas
     * Body JSON: { "habilitado": true, "limiteCpu": 90,
     *              "limiteTemperatura": 85, "limiteMemoria": 90 }
     * Retorna: JSON com status "ok".
     */
    private static void atualizarAlertas(Context ctx) {
        try {
            ObjectNode body = MAPPER.readTree(ctx.body()).deepCopy();
            var config = ServicoMonitoramento.obterInstancia().getConfiguracaoAlerta();
            config.definirHabilitado(body.path("habilitado").asBoolean());
            config.definirLimiteCpu(body.path("limiteCpu").asDouble(90));
            config.definirLimiteTemperatura(body.path("limiteTemperatura").asDouble(85));
            config.definirLimiteMemoria(body.path("limiteMemoria").asDouble(90));
            ctx.json(Map.of("status", "ok"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }
}
