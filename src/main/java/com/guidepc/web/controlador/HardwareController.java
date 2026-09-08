package com.guidepc.web.controlador;

import com.guidepc.modelo.InformacoesHardware;
import com.guidepc.servico.ServicoColetorHardware;
import io.javalin.http.Context;
import io.javalin.config.RoutesConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller de hardware — expõe dados do sistema via REST.
 *
 * Rotas registradas:
 *   GET /api/hardware       → retorna informações completas de hardware
 *   GET /api/hardware/cpu   → retorna apenas dados da CPU
 *   GET /api/hardware/memoria → retorna apenas dados da memória
 *   GET /api/hardware/gpu   → retorna apenas dados da GPU
 */
public final class HardwareController {

    private HardwareController() {
    }

    /**
     * Registra todas as rotas deste controller no Javalin.
     */
    public static void registrar(RoutesConfig routes) {
        routes.get("/api/hardware", HardwareController::obterHardware);
        routes.get("/api/hardware/cpu", HardwareController::obterCpu);
        routes.get("/api/hardware/memoria", HardwareController::obterMemoria);
        routes.get("/api/hardware/gpu", HardwareController::obterGpu);
        routes.get("/api/hardware/disco-espaco", HardwareController::obterDiscoEspaco);
    }

    /**
     * Retorna informações completas de hardware do sistema.
     *
     * Rota: GET /api/hardware
     * Retorna: JSON com chaves "processador", "memoria", "discos",
     *          "placaMae", "sistemaOperacional" e "gpus".
     */
    private static void obterHardware(Context ctx) {
        try {
            ServicoColetorHardware coletor = ServicoColetorHardware.obterInstancia();
            InformacoesHardware hw = coletor.coletarTudo();

            Map<String, Object> resultado = new LinkedHashMap<>();

            Map<String, Object> processador = new LinkedHashMap<>();
            processador.put("fabricante", hw.processador().fabricante());
            processador.put("modelo", hw.processador().modelo());
            processador.put("microarquitetura", hw.processador().microarquitetura());
            processador.put("nucleosFisicos", hw.processador().nucleosFisicos());
            processador.put("nucleosLogicos", hw.processador().nucleosLogicos());
            processador.put("frequenciaBaseHz", hw.processador().frequenciaBaseHz());
            processador.put("frequenciaMaximaHz", hw.processador().frequenciaMaximaHz());
            processador.put("percentualUso", hw.processador().percentualUso());
            processador.put("temperaturaCelsius", hw.processador().temperaturaCelsius());
            resultado.put("processador", processador);

            Map<String, Object> memoria = new LinkedHashMap<>();
            memoria.put("totalBytes", hw.memoria().totalBytes());
            memoria.put("disponivelBytes", hw.memoria().disponivelBytes());
            memoria.put("emUsoBytes", hw.memoria().emUsoBytes());
            memoria.put("percentualUso", hw.memoria().percentualUso());
            resultado.put("memoria", memoria);

            resultado.put("discos", hw.discos().stream().map(d -> {
                Map<String, Object> disco = new LinkedHashMap<>();
                disco.put("nome", d.nome());
                disco.put("modelo", d.modelo());
                disco.put("tamanhoBytes", d.tamanhoBytes());
                disco.put("tipoInferido", d.tipoInferido());
                disco.put("pontosMontagem", d.pontosMontagem());
                return disco;
            }).toList());

            Map<String, Object> placaMae = new LinkedHashMap<>();
            placaMae.put("fabricante", hw.placaMae().fabricante());
            placaMae.put("modelo", hw.placaMae().modelo());
            placaMae.put("versaoBios", hw.placaMae().versaoBios());
            resultado.put("placaMae", placaMae);

            Map<String, Object> so = new LinkedHashMap<>();
            so.put("familia", hw.sistemaOperacional().familia());
            so.put("versao", hw.sistemaOperacional().versao());
            so.put("arquitetura", hw.sistemaOperacional().arquitetura());
            so.put("tempoAtividadeSegundos", hw.sistemaOperacional().tempoAtividadeSegundos());
            resultado.put("sistemaOperacional", so);

            resultado.put("gpus", hw.nomesGpu());

            ctx.json(resultado);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Retorna informações detalhadas da CPU.
     *
     * Rota: GET /api/hardware/cpu
     * Retorna: JSON com usoPercentual, temperaturaCelsius, frequenciaMaximaHz,
     *          nucleosFisicos e nucleosLogicos.
     */
    private static void obterCpu(Context ctx) {
        try {
            ServicoColetorHardware coletor = ServicoColetorHardware.obterInstancia();
            var proc = coletor.obterInformacoesProcessador();
            Map<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("usoPercentual", proc.percentualUso());
            resultado.put("temperaturaCelsius", proc.temperaturaCelsius());
            resultado.put("frequenciaMaximaHz", proc.frequenciaMaximaHz());
            resultado.put("nucleosFisicos", proc.nucleosFisicos());
            resultado.put("nucleosLogicos", proc.nucleosLogicos());
            ctx.json(resultado);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Retorna informações de memória RAM.
     *
     * Rota: GET /api/hardware/memoria
     * Retorna: JSON com totalBytes, disponivelBytes, emUsoBytes e percentualUso.
     */
    private static void obterMemoria(Context ctx) {
        try {
            ServicoColetorHardware coletor = ServicoColetorHardware.obterInstancia();
            var mem = coletor.obterInformacoesMemoria();
            Map<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("totalBytes", mem.totalBytes());
            resultado.put("disponivelBytes", mem.disponivelBytes());
            resultado.put("emUsoBytes", mem.emUsoBytes());
            resultado.put("percentualUso", mem.percentualUso());
            ctx.json(resultado);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Retorna informações de GPU (placa de vídeo).
     *
     * Rota: GET /api/hardware/gpu
     * Retorna: JSON com nomes (lista de GPUs), usoPercentual e temperaturaCelsius.
     */
    private static void obterGpu(Context ctx) {
        try {
            ServicoColetorHardware coletor = ServicoColetorHardware.obterInstancia();
            Map<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("nomes", coletor.obterNomesGpu());
            resultado.put("usoPercentual", coletor.obterUsoGpu());
            resultado.put("temperaturaCelsius", coletor.obterTemperaturaGpu());
            ctx.json(resultado);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }

    private static void obterDiscoEspaco(Context ctx) {
        try {
            ServicoColetorHardware coletor = ServicoColetorHardware.obterInstancia();
            var particoes = coletor.obterEspacoDisco();
            ctx.json(particoes);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("erro", e.getMessage()));
        }
    }
}
