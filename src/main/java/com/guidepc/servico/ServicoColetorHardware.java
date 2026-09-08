package com.guidepc.servico;

import com.guidepc.modelo.InformacoesBateria;
import com.guidepc.modelo.InformacoesDisco;
import com.guidepc.modelo.InformacoesDiscoParticao;
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
import oshi.hardware.PowerSource;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Fachada para leitura de hardware via OSHI.
 *
 * <p>Singleton thread-safe: todas as leituras que acessam o HAL sao sincronizadas,
 * pois o OSHI nao garante seguranca para chamadas concorrentes. O calculo de
 * carga de CPU usa ticks entre chamadas para maior precisao.</p>
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
        if (instanciaUnica == null) {
            instanciaUnica = new ServicoColetorHardware();
        }
        return instanciaUnica;
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

        // Fallback: se maxFreq vier 0 (comum em VMs), usa vendorFreq
        long frequenciaMaximaHz = frequenciaMaxima != 0 ? frequenciaMaxima : identificador.getVendorFreq();
        long frequenciaBaseHz = frequenciaMaximaHz;

        double percentualUso = this.obterPercentualCargaCpu();
        double temperatura;
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

    /**
     * Calcula carga de CPU. Se a ultima coleta foi ha menos de 250 ms,
     * usa medicao com delay (mais estavel); caso contrario usa delta de ticks.
     */
    public synchronized double obterPercentualCargaCpu() {
        try {
            long instanteAtual = System.currentTimeMillis();
            long diferencaMillis = instanteAtual - this.instanteAnteriorMillis;

            if (diferencaMillis < 250) {
                return this.camadaHardware.getProcessor().getSystemCpuLoad(300) * 100.0;
            }

            double carga = this.camadaHardware.getProcessor().getSystemCpuLoadBetweenTicks(this.ticksAnteriores) * 100.0;
            this.ticksAnteriores = this.camadaHardware.getProcessor().getSystemCpuLoadTicks();
            this.instanteAnteriorMillis = instanteAtual;

            double cargaLimitada = Math.max(0.0, Math.min(100.0, carga));
            if (Double.isNaN(cargaLimitada)) {
                return 0.0;
            }
            return cargaLimitada;
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
            } catch (Exception ignored) {
                // updateAttributes pode falhar sem permissao SMART; segue com dados em cache
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

    /**
     * Tenta obter o uso da GPU via nvidia-smi (NVIDIA) ou WMI (Windows).
     * Retorna NaN se nao for possivel obter.
     */
    public synchronized double obterUsoGpu() {
        try {
            String sistema = System.getProperty("os.name", "").toLowerCase();
            if (sistema.contains("win")) {
                return this.obterUsoGpuWindows();
            } else {
                return this.obterUsoGpuLinux();
            }
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * Tenta obter a temperatura da GPU via nvidia-smi ou WMI.
     * Retorna NaN se nao for possivel obter.
     */
    public synchronized double obterTemperaturaGpu() {
        try {
            String sistema = System.getProperty("os.name", "").toLowerCase();
            if (sistema.contains("win")) {
                return this.obterTemperaturaGpuWindows();
            } else {
                return this.obterTemperaturaGpuLinux();
            }
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private double obterUsoGpuWindows() {
        try {
            ProcessBuilder pb = new ProcessBuilder("wmic", "path", "win32_videocontroller",
                    "get", "AdapterRAM,Name", "/format:list");
            pb.redirectErrorStream(true);
            Process processo = pb.start();
            String saida;
            try (var is = processo.getInputStream()) {
                saida = new String(is.readAllBytes());
            }
            boolean concluido = processo.waitFor(5, TimeUnit.SECONDS);
            if (!concluido) {
                processo.destroyForcibly();
                return Double.NaN;
            }
            // parsing basico do wmic: procura linha "AdapterRAM=XXXXX"
            for (String linha : saida.split("\n")) {
                String trim = linha.trim();
                if (trim.startsWith("AdapterRAM=")) {
                    String valor = trim.substring("AdapterRAM=".length()).trim();
                    if (!valor.isEmpty()) {
                        long bytes = Long.parseLong(valor);
                        // AdapterRAM retorna em bytes, mas o campo nao e uso - retorna NaN (sem suporte nativo)
                    }
                }
            }
            return Double.NaN;
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private double obterUsoGpuLinux() {
        try {
            ProcessBuilder pb = new ProcessBuilder("nvidia-smi",
                    "--query-gpu=utilization.gpu", "--format=csv,noheader,nounits");
            pb.redirectErrorStream(true);
            Process processo = pb.start();
            String saida;
            try (var is = processo.getInputStream()) {
                saida = new String(is.readAllBytes()).trim();
            }
            boolean concluido = processo.waitFor(5, TimeUnit.SECONDS);
            if (!concluido) {
                processo.destroyForcibly();
                return Double.NaN;
            }
            if (!saida.isEmpty()) {
                return Double.parseDouble(saida.split("\n")[0].trim());
            }
        } catch (Exception ignored) {
        }
        return Double.NaN;
    }

    private double obterTemperaturaGpuWindows() {
        try {
            ProcessBuilder pb = new ProcessBuilder("wmic", "path",
                    "Win32_PerfFormattedData_ThermalZoneInformation",
                    "get", "Temperature", "/format:list");
            pb.redirectErrorStream(true);
            Process processo = pb.start();
            String saida;
            try (var is = processo.getInputStream()) {
                saida = new String(is.readAllBytes());
            }
            boolean concluido = processo.waitFor(5, TimeUnit.SECONDS);
            if (!concluido) {
                processo.destroyForcibly();
                return Double.NaN;
            }
            // WMI retorna temperatura em deciKelvin (ex: 3122 = 312.2K = ~39C)
            for (String linha : saida.split("\n")) {
                String trim = linha.trim();
                if (trim.startsWith("Temperature=")) {
                    String valor = trim.substring("Temperature=".length()).trim();
                    if (!valor.isEmpty()) {
                        double kelvin = Double.parseDouble(valor) / 10.0;
                        return kelvin - 273.15;
                    }
                }
            }
            return Double.NaN;
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private double obterTemperaturaGpuLinux() {
        try {
            ProcessBuilder pb = new ProcessBuilder("nvidia-smi",
                    "--query-gpu=temperature.gpu", "--format=csv,noheader,nounits");
            pb.redirectErrorStream(true);
            Process processo = pb.start();
            String saida;
            try (var is = processo.getInputStream()) {
                saida = new String(is.readAllBytes()).trim();
            }
            boolean concluido = processo.waitFor(5, TimeUnit.SECONDS);
            if (!concluido) {
                processo.destroyForcibly();
                return Double.NaN;
            }
            if (!saida.isEmpty()) {
                return Double.parseDouble(saida.split("\n")[0].trim());
            }
        } catch (Exception ignored) {
        }
        return Double.NaN;
    }

    private String protegerTexto(String textoOriginal) {
        return Optional.ofNullable(textoOriginal)
                .filter(texto -> !texto.isBlank())
                .filter(texto -> !texto.equalsIgnoreCase("unknown"))
                .orElse("Nao disponivel");
    }

    private String formatarVram(long vramBytes) {
        if (vramBytes == 0) {
            return "N/A";
        }
        return Formatador.formatarBytes(vramBytes);
    }

    public SystemInfo obterInformacoesSistema() {
        return this.informacoesSistema;
    }

    public HardwareAbstractionLayer obterCamadaHardware() {
        return this.camadaHardware;
    }

    /**
     * Obtem dados da bateria do sistema (notebooks).
     * Retorna null se nao houver bateria (desktops).
     */
    public synchronized InformacoesBateria obterBateria() {
        try {
            for (PowerSource ps : this.camadaHardware.getPowerSources()) {
                double percentual = ps.getRemainingCapacityPercent();
                boolean carregando = ps.isCharging();
                long tempoRestante = -1;
                try {
                    tempoRestante = (long) ps.getTimeRemainingInstant();
                } catch (Exception ignored) {
                }
                return new InformacoesBateria(percentual, carregando, tempoRestante);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Obtem espaco em disco de todas as particoes/montagens.
     */
    public synchronized List<InformacoesDiscoParticao> obterEspacoDisco() {
        List<InformacoesDiscoParticao> particoes = new ArrayList<>();
        try {
            for (OSFileStore fs : this.sistemaOperacional.getFileSystem().getFileStores()) {
                long total = fs.getTotalSpace();
                long livre = fs.getUsableSpace();
                long usado = total - livre;
                String mount = fs.getVolume();
                if (mount == null || mount.isBlank()) {
                    mount = fs.getMount();
                }
                particoes.add(new InformacoesDiscoParticao(mount, total, usado, livre));
            }
        } catch (Exception ignored) {
        }
        return particoes;
    }
}
