package com.guidepc.modelo;

/**
 * Nivel de carga aplicado durante o teste de estresse.
 *
 * <ul>
 *   <li>{@link #NORMAL} - apenas monitora, sem carga artificial (baseline).</li>
 *   <li>{@link #BAIXO} - ocupa ~50% dos nucleos logicos com calculo matematico continuo.</li>
 *   <li>{@link #ALTO} - ocupa 100% dos nucleos + aloca memoria ate 60% da RAM livre, limitado a 2 GB.</li>
 * </ul>
 */
public enum NivelEstresse {

    NORMAL("Normal (base)", "Sem carga artificial - monitora uso em repouso", 0.0, false),
    BAIXO("Baixo estresse", "50% dos nucleos com carga matematica moderada", 0.5, false),
    ALTO("Alto estresse", "100% dos nucleos + alocacao de memoria ate limite seguro", 1.0, true);

    private final String rotulo;
    private final String descricao;
    private final double fatorCpu;
    private final boolean alocarMemoria;

    NivelEstresse(String rotulo, String descricao, double fatorCpu, boolean alocarMemoria) {
        this.rotulo = rotulo;
        this.descricao = descricao;
        this.fatorCpu = fatorCpu;
        this.alocarMemoria = alocarMemoria;
    }

    public String obterRotulo() {
        return this.rotulo;
    }

    public String obterDescricao() {
        return this.descricao;
    }

    public double obterFatorCpu() {
        return this.fatorCpu;
    }

    /** Indica se o nivel deve tambem pressionar memoria (apenas ALTO). */
    public boolean deveAlocarMemoria() {
        return this.alocarMemoria;
    }

    @Override
    public String toString() {
        return this.rotulo;
    }
}
