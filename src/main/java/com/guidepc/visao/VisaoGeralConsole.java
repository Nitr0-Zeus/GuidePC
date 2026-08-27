package com.guidepc.visao;

import com.guidepc.modelo.InformacoesDisco;
import com.guidepc.modelo.InformacoesHardware;
import com.guidepc.utilitario.Formatador;

import java.util.List;

/** Renderiza a visao geral de hardware em secoes (CPU, RAM, discos, placa-mae, SO, GPU). */
public final class VisaoGeralConsole {

    private VisaoGeralConsole() {
    }

    public static void exibir(InformacoesHardware informacoesHardware) {
        VisaoConsole.exibirCabecalho("VISAO GERAL - HARDWARE DETECTADO");
        VisaoConsole.exibirLinha("Dados coletados via OSHI (sem acesso a dados privados)");
        VisaoConsole.exibirSeparador();

        VisaoConsole.exibirLinha("[PROCESSADOR]");
        VisaoConsole.exibirLinha("  Fabricante e Modelo : " + informacoesHardware.processador().fabricante() + " " + informacoesHardware.processador().modelo());
        VisaoConsole.exibirLinha("  Microarquitetura    : " + informacoesHardware.processador().microarquitetura());
        VisaoConsole.exibirLinha("  Nucleos Fisicos     : " + informacoesHardware.processador().nucleosFisicos());
        VisaoConsole.exibirLinha("  Nucleos Logicos     : " + informacoesHardware.processador().nucleosLogicos());
        VisaoConsole.exibirLinha("  Pacotes Fisicos     : " + informacoesHardware.processador().pacotesFisicos());
        VisaoConsole.exibirLinha("  Frequencia Base     : " + Formatador.formatarFrequencia(informacoesHardware.processador().frequenciaBaseHz()));
        VisaoConsole.exibirLinha("  Frequencia Maxima   : " + Formatador.formatarFrequencia(informacoesHardware.processador().frequenciaMaximaHz()));
        VisaoConsole.exibirLinha("  Uso Atual           : " + Formatador.formatarPercentual(informacoesHardware.processador().percentualUso()));
        VisaoConsole.exibirLinha("  Temperatura         : " + Formatador.formatarTemperatura(informacoesHardware.processador().temperaturaCelsius()));
        VisaoConsole.exibirSeparador();

        VisaoConsole.exibirLinha("[MEMORIA RAM]");
        VisaoConsole.exibirLinha("  Total               : " + Formatador.formatarBytes(informacoesHardware.memoria().totalBytes()));
        VisaoConsole.exibirLinha("  Disponivel          : " + Formatador.formatarBytes(informacoesHardware.memoria().disponivelBytes()));
        VisaoConsole.exibirLinha("  Em Uso              : " + Formatador.formatarBytes(informacoesHardware.memoria().emUsoBytes()) + " (" + Formatador.formatarPercentual(informacoesHardware.memoria().percentualUso()) + ")");
        VisaoConsole.exibirLinha("  Tamanho Pagina      : " + Formatador.formatarBytes(informacoesHardware.memoria().tamanhoPaginaBytes()));
        VisaoConsole.exibirSeparador();

        VisaoConsole.exibirLinha("[DISCOS]");
        List<InformacoesDisco> listaDiscos = informacoesHardware.discos();
        if (listaDiscos.isEmpty()) {
            VisaoConsole.exibirLinha("  Nenhum disco detectado ou sem permissao");
        } else {
            for (InformacoesDisco discoAtual : listaDiscos) {
                String pontosMontagem = discoAtual.pontosMontagem().isEmpty() ? "sem ponto de montagem" : String.join(", ", discoAtual.pontosMontagem());
                VisaoConsole.exibirLinha("  - " + discoAtual.nome() + " | " + discoAtual.modelo() + " | " + Formatador.formatarBytes(discoAtual.tamanhoBytes()) + " | " + discoAtual.tipoInferido() + " | Montagem: " + pontosMontagem);
                VisaoConsole.exibirLinha("    Leituras: " + discoAtual.leituras() + " Escritas: " + discoAtual.escritas());
            }
        }
        VisaoConsole.exibirSeparador();

        VisaoConsole.exibirLinha("[PLACA MAE / BIOS]");
        VisaoConsole.exibirLinha("  Fabricante Placa    : " + informacoesHardware.placaMae().fabricante());
        VisaoConsole.exibirLinha("  Modelo Placa        : " + informacoesHardware.placaMae().modelo());
        VisaoConsole.exibirLinha("  Versao Placa        : " + informacoesHardware.placaMae().versao());
        VisaoConsole.exibirLinha("  Serial Placa        : " + informacoesHardware.placaMae().numeroSerial());
        VisaoConsole.exibirLinha("  Fabricante BIOS     : " + informacoesHardware.placaMae().fabricanteBios());
        VisaoConsole.exibirLinha("  Versao BIOS         : " + informacoesHardware.placaMae().versaoBios());
        VisaoConsole.exibirLinha("  Data BIOS           : " + informacoesHardware.placaMae().dataLancamentoBios());
        VisaoConsole.exibirSeparador();

        VisaoConsole.exibirLinha("[SISTEMA OPERACIONAL]");
        VisaoConsole.exibirLinha("  Familia             : " + informacoesHardware.sistemaOperacional().familia());
        VisaoConsole.exibirLinha("  Versao              : " + informacoesHardware.sistemaOperacional().versao());
        VisaoConsole.exibirLinha("  Codinome            : " + informacoesHardware.sistemaOperacional().codinome());
        VisaoConsole.exibirLinha("  Build               : " + informacoesHardware.sistemaOperacional().numeroBuild());
        VisaoConsole.exibirLinha("  Arquitetura         : " + informacoesHardware.sistemaOperacional().arquitetura());
        VisaoConsole.exibirLinha("  Tempo Atividade     : " + Formatador.formatarTempoAtividade(informacoesHardware.sistemaOperacional().tempoAtividadeSegundos()));
        VisaoConsole.exibirSeparador();

        VisaoConsole.exibirLinha("[PLACA GRAFICA - GPU]");
        List<String> listaGpu = informacoesHardware.nomesGpu();
        if (listaGpu.isEmpty()) {
            VisaoConsole.exibirLinha("  Nao disponivel");
        } else {
            for (String nomeGpu : listaGpu) {
                VisaoConsole.exibirLinha("  - " + nomeGpu);
            }
        }
        VisaoConsole.exibirSeparador();
    }
}
