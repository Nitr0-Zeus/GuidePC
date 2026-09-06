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
            resultado.put("processador", new LinkedHashMap<>() {{
                put("fabricante", hw.processador().fabricante());
                put("modelo", hw.processador().modelo());
                put("microarquitetura", hw.processador().microarquitetura());
                put("nucleosFisicos", hw.processador().nucleosFisicos());
                put("nucleosLogicos", hw.processador().nucleosLogicos());
                put("frequenciaBaseHz", hw.processador().frequenciaBaseHz());
                put("frequenciaMaximaHz", hw.processador().frequenciaMaximaHz());
                put("percentualUso", hw.processador().percentualUso());
                put("temperaturaCelsius", hw.processador().temperaturaCelsius());
            }});
            resultado.put("memoria", new LinkedHashMap<>() {{
                put("totalBytes", hw.memoria().totalBytes());
                put("disponivelBytes", hw.memoria().disponivelBytes());
                put("emUsoBytes", hw.memoria().emUsoBytes());
                put("percentualUso", hw.memoria().percentualUso());
            }});
            resultado.put("discos", hw.discos().stream().map(d -> {
                Map<String, Object> disco = new LinkedHashMap<>();
                disco.put("nome", d.nome());
                disco.put("modelo", d.modelo());
                disco.put("tamanhoBytes", d.tamanhoBytes());
                disco.put("tipoInferido", d.tipoInferido());
                disco.put("pontosMontagem", d.pontosMontagem());
                return disco;
            }).toList());
            resultado.put("placaMae", new LinkedHashMap<>() {{
                put("fabricante", hw.placaMae().fabricante());
                put("modelo", hw.placaMae().modelo());
                put("versaoBios", hw.placaMae().versaoBios());
            }});
            resultado.put("sistemaOperacional", new LinkedHashMap<>() {{
                put("familia", hw.sistemaOperacional().familia());
                put("versao", hw.sistemaOperacional().versao());
                put("arquitetura", hw.sistemaOperacional().arquitetura());
                put("tempoAtividadeSegundos", hw.sistemaOperacional().tempoAtividadeSegundos());
            }});
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
}
