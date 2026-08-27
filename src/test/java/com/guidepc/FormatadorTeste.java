package com.guidepc;

import com.guidepc.utilitario.Formatador;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatadorTeste {

    @Test
    void deveFormatarBytesCorretamente() {
        String resultado512 = Formatador.formatarBytes(512);
        assertEquals("512 B", resultado512);

        String resultadoKb = Formatador.formatarBytes(1024);
        assertTrue(resultadoKb.contains("KB"));

        String resultadoGb = Formatador.formatarBytes(1024L * 1024 * 1024);
        assertTrue(resultadoGb.contains("GB"));
    }

    @Test
    void deveFormatarFrequenciaCorretamente() {
        String resultadoZero = Formatador.formatarFrequencia(0);
        assertEquals("Nao disponivel", resultadoZero);

        String resultadoGhz = Formatador.formatarFrequencia(3600000000L);
        assertTrue(resultadoGhz.contains("GHz"));
    }

    @Test
    void deveFormatarPercentualCorretamente() {
        String resultadoPercentual = Formatador.formatarPercentual(42.5);
        assertTrue(resultadoPercentual.contains("42"));
        assertTrue(resultadoPercentual.contains("%"));
    }

    @Test
    void deveFormatarTempoAtividadeCorretamente() {
        String resultadoUptime = Formatador.formatarTempoAtividade(3661);
        assertTrue(resultadoUptime.contains("1h"));
    }
}
