package com.guidepc.modelo;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * InformacoesDisco - Modelo imutavel. Campos mapeados do OSHI, usados em Servico e Visao.
 */
public record InformacoesDisco(
        String nome,
        String modelo,
        String serial,
        long tamanhoBytes,
        long leituras,
        long escritas,
        long bytesLidos,
        long bytesEscritos,
        double tempoTransferenciaMs,
        List<String> pontosMontagem,
        String tipoInferido
) {

    public static String inferirTipo(String modeloDisco) {
        return Optional.ofNullable(modeloDisco)
                .map(textoModelo -> textoModelo.toUpperCase(Locale.ROOT))
                .map(textoMaiusculo -> {
                    // Usa switch para evitar if encadeado
                    boolean contemNvme = textoMaiusculo.contains("NVME");
                    boolean contemSsd = textoMaiusculo.contains("SSD");
                    return switch (Boolean.valueOf(contemNvme).toString() + Boolean.valueOf(contemSsd).toString()) {
                        case "truetrue", "truefalse" -> "NVMe SSD";
                        case "falsetrue" -> "SSD";
                        default -> "HDD";
                    };
                })
                .orElse("Desconhecido");
    }

    // Sobrecarga que trata nulo com Optional, sem if
    public static String inferirTipoSeguro(String modeloDisco) {
        return Optional.ofNullable(modeloDisco)
                .filter(textoModelo -> !textoModelo.isBlank())
                .map(InformacoesDisco::inferirTipo)
                .orElse("Desconhecido");
    }
}
