package com.guidepc.utilitario;

import java.util.Locale;
import java.util.Optional;

/**
 * Formatacao de valores para exibicao em console no padrao pt-BR.
 * Retorna "Nao disponivel" quando o valor e invalido (NaN ou 0 quando aplicavel).
 */
public final class Formatador {

    private static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");

    private Formatador() {
    }

    public static String formatarBytes(long quantidadeBytes) {
        if (quantidadeBytes == -1) {
            return "Nao disponivel";
        }
        if (quantidadeBytes < 1024) {
            return quantidadeBytes + " B";
        }
        if (quantidadeBytes < 1024 * 1024) {
            return String.format(LOCALE_BR, "%.1f KB", quantidadeBytes / 1024.0);
        }
        if (quantidadeBytes < 1024L * 1024 * 1024) {
            return String.format(LOCALE_BR, "%.1f MB", quantidadeBytes / (1024.0 * 1024));
        }
        double gigabytes = quantidadeBytes / (1024.0 * 1024 * 1024);
        if (gigabytes < 1024) {
            return String.format(LOCALE_BR, "%.2f GB", gigabytes);
        }
        return String.format(LOCALE_BR, "%.2f TB", gigabytes / 1024.0);
    }

    public static String formatarFrequencia(long frequenciaHz) {
        if (frequenciaHz == 0) {
            return "Nao disponivel";
        }
        double gigahertz = frequenciaHz / 1000000000.0;
        if (gigahertz >= 1.0) {
            return String.format(LOCALE_BR, "%.2f GHz", gigahertz);
        }
        return String.format(LOCALE_BR, "%.0f MHz", frequenciaHz / 1000000.0);
    }

    public static String formatarPercentual(double valorPercentual) {
        if (Double.isNaN(valorPercentual)) {
            return "Nao disponivel";
        }
        return String.format(LOCALE_BR, "%.1f%%", valorPercentual);
    }

    public static String formatarTemperatura(double temperaturaCelsius) {
        if (Double.isNaN(temperaturaCelsius) || temperaturaCelsius == 0.0) {
            return "Nao disponivel";
        }
        return String.format(LOCALE_BR, "%.1f \u00B0C", temperaturaCelsius);
    }

    public static String formatarTempoAtividade(long segundosTotais) {
        if (segundosTotais == 0) {
            return "Nao disponivel";
        }
        long dias = segundosTotais / 86400;
        long horas = (segundosTotais % 86400) / 3600;
        long minutos = (segundosTotais % 3600) / 60;
        if (dias > 0) {
            return String.format("%dd %dh %dm", dias, horas, minutos);
        }
        if (horas > 0) {
            return String.format("%dh %dm", horas, minutos);
        }
        return String.format("%dm", minutos);
    }

    public static String protegerTexto(String textoOriginal) {
        return Optional.ofNullable(textoOriginal)
                .filter(texto -> !texto.isBlank())
                .filter(texto -> !texto.equalsIgnoreCase("unknown"))
                .orElse("Nao disponivel");
    }

    public static String formatarTempoMs(double tempoMs) {
        if (Double.isNaN(tempoMs)) {
            return "N/A";
        }
        if (tempoMs < 1.0) {
            return String.format(LOCALE_BR, "%.2f ms", tempoMs);
        }
        return String.format(LOCALE_BR, "%.1f ms", tempoMs);
    }
}
