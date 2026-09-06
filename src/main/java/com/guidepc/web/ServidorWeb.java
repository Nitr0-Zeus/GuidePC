package com.guidepc.web;

import com.guidepc.web.controlador.AlertaController;
import com.guidepc.web.controlador.HardwareController;
import com.guidepc.web.controlador.HistoricoController;
import com.guidepc.web.controlador.RelatorioController;
import com.guidepc.web.controlador.TesteController;
import com.guidepc.web.websocket.MonitoramentoWs;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

/**
 * Configura e inicia o servidor web Javalin.
 */
public final class ServidorWeb {

    private static Javalin app;

    private ServidorWeb() {
    }

    /**
     * Cria e inicia o servidor Javalin na porta especificada.
     * Configura arquivos estáticos, rotas HTTP e WebSocket.
     */
    public static void iniciar(int porta) {
        app = Javalin.create(config -> {
            // Oculta o banner de inicialização do Javalin no console
            config.startup.showJavalinBanner = false;

            // Serve arquivos estáticos (HTML, CSS, JS) da pasta /public no classpath
            config.staticFiles.add("/public", Location.CLASSPATH);

            // Define o tipo de conteúdo padrão das respostas HTTP como JSON
            config.http.defaultContentType = "application/json";

            // Registra as rotas REST de cada controller
            HardwareController.registrar(config.routes);
            TesteController.registrar(config.routes);
            HistoricoController.registrar(config.routes);
            AlertaController.registrar(config.routes);
            RelatorioController.registrar(config.routes);

            // Registra o WebSocket para monitoramento em tempo real
            config.routes.ws("/ws/monitoramento", ws -> {
                ws.onConnect(ctx -> MonitoramentoWs.onConnect(ctx));
                ws.onClose(ctx -> MonitoramentoWs.onClose(ctx));
                ws.onMessage(ctx -> MonitoramentoWs.onMessage(ctx, ctx.message()));
            });
        });

        // Inicia o servidor na porta configurada
        app.start(porta);
    }

    /** Para o servidor Javalin se estiver rodando. */
    public static void parar() {
        if (app != null) {
            app.stop();
        }
    }

    /** Retorna a instância do Javalin (usado para testes). */
    public static Javalin obterApp() {
        return app;
    }
}
