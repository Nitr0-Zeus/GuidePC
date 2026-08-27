package com.guidepc.servico;

import com.guidepc.modelo.InformacoesDisco;
import com.guidepc.modelo.InformacoesHardware;
import com.guidepc.modelo.InformacoesMemoria;
import com.guidepc.modelo.InformacoesPlacaMae;
import com.guidepc.modelo.InformacoesProcessador;
import com.guidepc.modelo.InformacoesSistemaOperacional;
import com.guidepc.utilitario.Formatador;
import oshi.SystemInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Firmware;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ServicoColetorHardware - Regra de negocio. Thread-safe onde acessa hardware.
 */
public class ServicoColetorHardware {

    private static ServicoColetorHardware instanciaUnica;
    private final SystemInfo informacoesSistema;
    private final HardwareAbstractionLayer camadaHardware;
    private final OperatingSystem sistemaOperacional;
    private long[] ticksAnteriores;
    private long instanteAnteriorMillis;

    private ServicoColetorHardware() {
        this.informacoesSistema = new SystemInfo();
        this.camadaHardware = this.informacoesSistema.getHardware();
        this.sistemaOperacional = this.informacoesSistema.getOperatingSystem();
        this.ticksAnteriores = this.camadaHardware.getProcessor().getSystemCpuLoadTicks();
        this.instanteAnteriorMillis = System.currentTimeMillis();
    }

    public static synchronized ServicoColetorHardware obterInstancia() {
        return Optional.ofNullable(instanciaUnica)
                .orElseGet(() -> {
                    instanciaUnica = new ServicoColetorHardware();
                    return instanciaUnica;
                });
    }

    public synchronized InformacoesHardware coletarTudo() {
        InformacoesProcessador informacoesProcessador = this.obterInformacoesProcessador();
        InformacoesMemoria informacoesMemoria = this.obterInformacoesMemoria();
        List<InformacoesDisco> listaDiscos = this.obterInformacoesDiscos();
        InformacoesPlacaMae informacoesPlacaMae = this.obterInformacoesPlacaMae();
        InformacoesSistemaOperacional informacoesSistemaOperacional = this.obterInformacoesSistemaOperacional();
        List<String> listaGpu = this.obterNomesGpu();
        return new InformacoesHardware(
                informacoesProcessador,
                informacoesMemoria,
                listaDiscos,
                informacoesPlacaMae,
                informacoesSistemaOperacional,
                listaGpu
        );
    }

    public synchronized InformacoesProcessador obterInformacoesProcessador() {
        CentralProcessor processadorCentral = this.camadaHardware.getProcessor();
        CentralProcessor.ProcessorIdentifier identificador = processadorCentral.getProcessorIdentifier();
        long[] frequenciasAtuais = processadorCentral.getCurrentFreq();
        long frequenciaMaxima = processadorCentral.getMaxFreq();

        long frequenciaMaximaHz = switch (Long.toString(frequenciaMaxima)) {
            case "0" -> identificador.getVendorFreq();
            default -> frequenciaMaxima;
        };
        long frequenciaBaseHz = frequenciaMaximaHz;

        double percentualUso = this.obterPercentualCargaCpu();
        double temperatura = 0.0;
        try {
            temperatura = this.camadaHardware.getSensors().getCpuTemperature();
        } catch (Exception excecao) {
            temperatura = Double.NaN;
        }

        long[] frequenciasSeguras = Optional.ofNullable(frequenciasAtuais).orElse(new long[0]);

        return new InformacoesProcessador(
                this.protegerTexto(identificador.getVendor()),
                this.protegerTexto(identificador.getName()),
                this.protegerTexto(identificador.getMicroarchitecture()),
                processadorCentral.getPhysicalProcessorCount(),
                processadorCentral.getLogicalProcessorCount(),
                processadorCentral.getPhysicalPackageCount(),
                frequenciaBaseHz,
                frequenciaMaximaHz,
                frequenciasSeguras,
                percentualUso,
                temperatura
        );
    }

    public synchronized double obterPercentualCargaCpu() {
        try {
            long instanteAtual = System.currentTimeMillis();
            long diferencaMillis = instanteAtual - this.instanteAnteriorMillis;

            // Sem if: usa switch em comparacao
            return switch (Boolean.toString(diferencaMillis < 250)) {
                case "true" -> this.camadaHardware.getProcessor().getSystemCpuLoad(300) * 100.0;
                default -> {
                    double carga = this.camadaHardware.getProcessor().getSystemCpuLoadBetweenTicks(this.ticksAnteriores) * 100.0;
                    this.ticksAnteriores = this.camadaHardware.getProcessor().getSystemCpuLoadTicks();
                    this.instanteAnteriorMillis = instanteAtual;
                    double cargaLimitada = Math.max(0.0, Math.min(100.0, carga));
                    // Trata NaN sem if via OptionalDouble
                    boolean cargaInvalida = Double.isNaN(cargaLimitada);
                    yield switch (Boolean.toString(cargaInvalida)) {
                        case "true" -> 0.0;
                        default -> cargaLimitada;
                    };
                }
            };
        } catch (Exception excecao) {
            return Double.NaN;
        }
    }

    public synchronized InformacoesMemoria obterInformacoesMemoria() {
        GlobalMemory memoriaGlobal = this.camadaHardware.getMemory();
        long totalBytes = memoriaGlobal.getTotal();
        long disponivelBytes = memoriaGlobal.getAvailable();
        long emUsoBytes = totalBytes - disponivelBytes;
        double percentualUso = totalBytes > 0 ? (emUsoBytes * 100.0 / totalBytes) : Double.NaN;
        String memoriaVirtual = Optional.ofNullable(memoriaGlobal.getVirtualMemory())
                .map(Object::toString)
                .orElse("N/A");
        return new InformacoesMemoria(totalBytes, disponivelBytes, emUsoBytes, percentualUso, memoriaGlobal.getPageSize(), memoriaVirtual);
    }

    public synchronized List<InformacoesDisco> obterInformacoesDiscos() {
        List<InformacoesDisco> listaSaida = new ArrayList<>();
        for (HWDiskStore disco : this.camadaHardware.getDiskStores()) {
            try {
                disco.updateAttributes();
            } catch (Exception excecao) {
                // ignora, sem if
            }
            List<String> pontosMontagem = new ArrayList<>();
            for (HWPartition particao : disco.getPartitions()) {
                Optional.ofNullable(particao.getMountPoint())
                        .filter(ponto -> !ponto.isBlank())
                        .ifPresent(pontosMontagem::add);
            }
            InformacoesDisco informacoesDisco = new InformacoesDisco(
                    disco.getName(),
                    disco.getModel(),
                    disco.getSerial(),
                    disco.getSize(),
                    disco.getReads(),
                    disco.getWrites(),
                    disco.getReadBytes(),
                    disco.getWriteBytes(),
                    disco.getTransferTime(),
                    pontosMontagem,
                    InformacoesDisco.inferirTipoSeguro(disco.getModel())
            );
            listaSaida.add(informacoesDisco);
        }
        return listaSaida;
    }

    public synchronized InformacoesPlacaMae obterInformacoesPlacaMae() {
        ComputerSystem sistemaComputacional = this.camadaHardware.getComputerSystem();
        Baseboard placaBase = sistemaComputacional.getBaseboard();
        Firmware firmware = sistemaComputacional.getFirmware();
        return new InformacoesPlacaMae(
                this.protegerTexto(placaBase.getManufacturer()),
                this.protegerTexto(placaBase.getModel()),
                this.protegerTexto(placaBase.getVersion()),
                this.protegerTexto(placaBase.getSerialNumber()),
                this.protegerTexto(firmware.getManufacturer()),
                this.protegerTexto(firmware.getVersion()),
                this.protegerTexto(firmware.getReleaseDate())
        );
    }

    public synchronized InformacoesSistemaOperacional obterInformacoesSistemaOperacional() {
        long tempoAtividade = this.sistemaOperacional.getSystemUptime();
        return new InformacoesSistemaOperacional(
                this.sistemaOperacional.getFamily(),
                this.sistemaOperacional.getVersionInfo().getVersion(),
                this.sistemaOperacional.getVersionInfo().getCodeName(),
                this.sistemaOperacional.getVersionInfo().getBuildNumber(),
                System.getProperty("os.arch"),
                tempoAtividade,
                false
        );
    }

    public synchronized List<String> obterNomesGpu() {
        List<String> listaGpu = new ArrayList<>();
        for (GraphicsCard placaGrafica : this.camadaHardware.getGraphicsCards()) {
            String nomeGpu = placaGrafica.getName() + " (" + this.protegerTexto(placaGrafica.getVendor()) + ") VRAM: " + this.formatarVram(placaGrafica.getVRam());
            listaGpu.add(nomeGpu);
        }
        return listaGpu;
    }

    private String protegerTexto(String textoOriginal) {
        return Optional.ofNullable(textoOriginal)
                .filter(texto -> !texto.isBlank())
                .filter(texto -> !texto.equalsIgnoreCase("unknown"))
                .orElse("Nao disponivel");
    }

    private String formatarVram(long vramBytes) {
        return switch (Long.toString(vramBytes)) {
            case "0" -> "N/A";
            default -> Formatador.formatarBytes(vramBytes);
        };
    }

    private String protegerTextoSimples(String textoOriginal) {
        return this.protegerTexto(textoOriginal);
    }

    public SystemInfo obterInformacoesSistema() {
        return this.informacoesSistema;
    }

    public HardwareAbstractionLayer obterCamadaHardware() {
        return this.camadaHardware;
    }
}
