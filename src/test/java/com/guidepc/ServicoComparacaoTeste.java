package com.guidepc;

import com.guidepc.modelo.Amostra;
import com.guidepc.modelo.NivelEstresse;
import com.guidepc.modelo.ResultadoTesteEstresse;
import com.guidepc.servico.ServicoComparacao;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicoComparacaoTeste {

    @Test
    void deveCalcularEstatisticasEExportarCsv() {
        List<Amostra> listaAmostras = List.of(
                new Amostra(0, 10.0, 50.0, 0L, 0.0, 2.0),
                new Amostra(500, 20.0, 55.0, 0L, 0.0, 3.0),
                new Amostra(1000, 30.0, 60.0, 0L, 0.0, 4.0)
        );

        ResultadoTesteEstresse resultadoTeste = new ResultadoTesteEstresse(
                NivelEstresse.BAIXO,
                30,
                Instant.now(),
                Instant.now(),
                listaAmostras
        );

        assertEquals(20.0, resultadoTeste.obterMediaCpu(), 0.01);
        assertEquals(30.0, resultadoTeste.obterMaximoCpu(), 0.01);

        ServicoComparacao servicoComparacao = new ServicoComparacao();
        servicoComparacao.armazenarResultado(resultadoTeste);

        String conteudoCsv = servicoComparacao.exportarCsv();
        assertTrue(conteudoCsv.contains("BAIXO"));

        String textoResumo = servicoComparacao.gerarTextoResumo();
        assertTrue(textoResumo.contains("Baixo"));
    }
}
