package com.guidepc.web.controlador;

import com.guidepc.servico.ServicoMonitoramento;
import io.javalin.http.Context;
import io.javalin.config.RoutesConfig;

import java.util.Map;

/**
 * Controller de alertas — gerencia limites configuráveis.
 *
 * Rotas registradas:
 *   GET /api/alertas/configuracao → obtém configuração atual de alertas
 *   PUT /api/alertas/configuracao → atualiza configuração de alertas
 */
public final class AlertaController {

    private AlertaController() {
    }

    /**
     * Registra todas as rotas deste controller no Javalin.
     */
    public static void registrar(RoutesConfig routes) {
        routes.get("/api/alertas/configuracao", AlertaController::obter);
        routes.put("/api/alertas/configuracao", AlertaController::atualizar);
    }

    /**
     * Obtém a configuração atual de alertas.
     *
     * Rota: GET /api/alertas/configuracao
     * Retorna: JSON com habilitado (boolean), limiteCpu, limiteTemperatura
     *          e limiteMemoria (valores double).
     */
    private static void obter(Context ctx) {
        var config = ServicoMonitoramento.obterInstancia().getConfiguracaoAlerta();
        ctx.json(Map.of(
            "habilitado", config.estaHabilitado(),
            "limiteCpu", config.obterLimiteCpu(),
            "limiteTemperatura", config.obterLimiteTemperatura(),
            "limiteMemoria", config.obterLimiteMemoria()
        ));
    }

    /**
     * Atualiza a configuração de alertas.
     *
     * Rota: PUT /api/alertas/configuracao
     * Body JSON: { "habilitado": true, "limiteCpu": 90.0,
     *              "limiteTemperatura": 85.0, "limiteMemoria": 90.0 }
     * Retorna: JSON com status "ok".
     */
    private static void atualizar(Context ctx) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            var config = ServicoMonitoramento.obterInstancia().getConfiguracaoAlerta();

            Object habilitado = body.get("habilitado");
            config.definirHabilitado(habilitado instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(habilitado)));
            config.definirLimiteCpu(toDouble(body.get("limiteCpu")));
            config.definirLimiteTemperatura(toDouble(body.get("limiteTemperatura")));
            config.definirLimiteMemoria(toDouble(body.get("limiteMemoria")));

            ctx.json(Map.of("status", "ok"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Método auxiliar para converter qualquer tipo numérico ou String para double.
     */
    private static double toDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(o));
    }
}
