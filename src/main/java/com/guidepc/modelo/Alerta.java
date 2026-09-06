package com.guidepc.modelo;

/**
 * Representa um alerta disparado quando um limite e excedido.
 */
public record Alerta(
        TipoAlerta tipo,
        double valorAtual,
        double limite,
        long instanteMillis
) {

    public enum TipoAlerta {
        CPU("CPU"),
        TEMPERATURA("Temperatura"),
        MEMORIA("Memoria");

        private final String rotulo;

        TipoAlerta(String rotulo) {
            this.rotulo = rotulo;
        }

        public String obterRotulo() {
            return this.rotulo;
        }
    }

    public String formatar() {
        return String.format("[ALERTA] %s %.1f%% > limite %.1f%%",
                this.tipo.obterRotulo(), this.valorAtual, this.limite);
    }
}
