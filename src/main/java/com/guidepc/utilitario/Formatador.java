package com.guidepc.utilitario;

/**
 * Formatador - Formatacao com Locale pt-BR. Retorna 'Nao disponivel' para NaN.
 */
public final class Formatador {

    private Formatador() {
    }

    public static String formatarBytes(long quantidadeBytes) {
        return switch (Long.toString(quantidadeBytes)) {
            case "-1" -> "Nao disponivel";
            default -> {
                // Usa switch em comparacao para evitar if
                boolean menorQueKb = quantidadeBytes < 1024;
                boolean menorQueMb = quantidadeBytes < 1024 * 1024;
                boolean menorQueGb = quantidadeBytes < 1024L * 1024 * 1024;
                String chave = Boolean.toString(menorQueKb) + "-" + Boolean.toString(menorQueMb) + "-" + Boolean.toString(menorQueGb);
                yield switch (chave) {
                    case "true-true-true" -> quantidadeBytes + " B";
                    case "false-true-true" -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f KB", quantidadeBytes / 1024.0);
                    case "false-false-true" -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f MB", quantidadeBytes / (1024.0 * 1024));
                    case "false-false-false" -> {
                        double gigabytes = quantidadeBytes / (1024.0 * 1024 * 1024);
                        boolean menorQueTb = gigabytes < 1024;
                        yield switch (Boolean.toString(menorQueTb)) {
                            case "true" -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f GB", gigabytes);
                            default -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f TB", gigabytes / 1024.0);
                        };
                    }
                    default -> quantidadeBytes + " B";
                };
            }
        };
    }

    public static String formatarFrequencia(long frequenciaHz) {
        return switch (Long.toString(frequenciaHz)) {
            case "0" -> "Nao disponivel";
            default -> {
                double gigahertz = frequenciaHz / 1000000000.0;
                boolean maiorQueUmGhz = gigahertz >= 1.0;
                yield switch (Boolean.toString(maiorQueUmGhz)) {
                    case "true" -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f GHz", gigahertz);
                    default -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.0f MHz", frequenciaHz / 1000000.0);
                };
            }
        };
    }

    public static String formatarPercentual(double valorPercentual) {
        boolean valorInvalido = Double.isNaN(valorPercentual);
        return switch (Boolean.toString(valorInvalido)) {
            case "true" -> "Nao disponivel";
            default -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f%%", valorPercentual);
        };
    }

    public static String formatarTemperatura(double temperaturaCelsius) {
        boolean temperaturaInvalida = Double.isNaN(temperaturaCelsius) || temperaturaCelsius == 0.0;
        return switch (Boolean.toString(temperaturaInvalida)) {
            case "true" -> "Nao disponivel";
            default -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f Â°C", temperaturaCelsius);
        };
    }

    public static String formatarTempoAtividade(long segundosTotais) {
        return switch (Long.toString(segundosTotais)) {
            case "0" -> "Nao disponivel";
            default -> {
                long dias = segundosTotais / 86400;
                long horas = (segundosTotais % 86400) / 3600;
                long minutos = (segundosTotais % 3600) / 60;
                boolean temDias = dias > 0;
                boolean temHoras = horas > 0;
                String chave = Boolean.toString(temDias) + "-" + Boolean.toString(temHoras);
                yield switch (chave) {
                    case "true-true", "true-false" -> String.format("%dd %dh %dm", dias, horas, minutos);
                    case "false-true" -> String.format("%dh %dm", horas, minutos);
                    default -> String.format("%dm", minutos);
                };
            }
        };
    }

    public static String protegerTexto(String textoOriginal) {
        return java.util.Optional.ofNullable(textoOriginal)
                .filter(texto -> !texto.isBlank())
                .filter(texto -> !texto.equalsIgnoreCase("unknown"))
                .orElse("Nao disponivel");
    }

    public static String formatarTempoMs(double tempoMs) {
        boolean tempoInvalido = Double.isNaN(tempoMs);
        return switch (Boolean.toString(tempoInvalido)) {
            case "true" -> "N/A";
            default -> {
                boolean menorQueUm = tempoMs < 1.0;
                yield switch (Boolean.toString(menorQueUm)) {
                    case "true" -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f ms", tempoMs);
                    default -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f ms", tempoMs);
                };
            }
        };
    }
}
