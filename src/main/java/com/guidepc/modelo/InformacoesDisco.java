package com.guidepc.modelo;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Dados de um disco fisico mapeados do OSHI.
 * O tipo (HDD/SSD/NVMe) e inferido pelo nome do modelo, pois o OSHI nao expoe o tipo diretamente.
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

    /**
     * Inicia pelo modelo do disco para estimar o tipo.
     * Heuristica simples: se contem "NVME" -> NVMe SSD; se contem "SSD" -> SSD; caso contrario HDD.
     */
    public static String inferirTipo(String modeloDisco) {
        if (modeloDisco == null) {
            return "Desconhecido";
        }
        String superior = modeloDisco.toUpperCase(Locale.ROOT);
        if (superior.contains("NVME")) {
            return "NVMe SSD";
        }
        if (superior.contains("SSD")) {
            return "SSD";
        }
        return "HDD";
    }

    /** Variante que trata nulo/branco e delega para {@link #inferirTipo(String)}. */
    public static String inferirTipoSeguro(String modeloDisco) {
        return Optional.ofNullable(modeloDisco)
                .filter(texto -> !texto.isBlank())
                .map(InformacoesDisco::inferirTipo)
                .orElse("Desconhecido");
    }
}
