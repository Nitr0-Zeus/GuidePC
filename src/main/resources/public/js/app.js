// ==================== GUIDEPC v4.0 — Script Principal do Aplicativo ====================

// ==================== ESTADO GLOBAL ====================
// Objeto que armazena todo o estado da aplicacao
const estado = {
    conexaoWebSocket: null,          // Conexao WebSocket ativa
    graficoCpu: null,                // Instancia do grafico de CPU
    graficoRam: null,                // Instancia do grafico de RAM
    graficoTemperatura: null,        // Instancia do grafico de temperatura
    graficoGpu: null,                // Instancia do grafico de GPU
    graficoDetalhe: null,            // Instancia do grafico de detalhe do teste
    dadosCpu: [],                    // Historico de valores de CPU
    dadosRam: [],                    // Historico de valores de RAM
    dadosTemperatura: [],            // Historico de valores de temperatura
    dadosGpu: [],                    // Historico de valores de GPU
    rotulosTempo: [],                // Rotulas de tempo para os graficos
    paginaAtual: 1,                  // Pagina atual do historico
    totalPaginas: 1,                 // Total de paginas do historico
    tentativasReconexao: 0,          // Contador de tentativas de reconexao
    maxTentativasReconexao: 20,      // Limite maximo de tentativas
    agendadorReconexao: null,        // Timeout para reconexao agendada
    instanciaGraficoDetalhe: null    // Instancia do grafico de detalhe do teste
};

// ==================== FUNCOES UTILITARIAS ====================

// Escapa caracteres especiais de HTML para evitar injecao de codigo
function escaparHtml(texto) {
    if (texto === null || texto === undefined) return '';
    const elemento = document.createElement('div');
    elemento.textContent = String(texto);
    return elemento.innerHTML;
}

// Converte bytes em formato legivel (KB, MB, GB, TB)
function formatarBytes(bytes) {
    if (bytes === 0) return '0 B';
    const unidades = ['B', 'KB', 'MB', 'GB', 'TB'];
    const indice = Math.floor(Math.log(Math.abs(bytes)) / Math.log(1024));
    return (bytes / Math.pow(1024, indice)).toFixed(1) + ' ' + unidades[indice];
}

// Formata frequencia em Hertz para formato legivel (MHz, GHz)
function formatarHertz(hertz) {
    if (!hertz || hertz === 0) return 'N/A';
    if (hertz >= 1e9) return (hertz / 1e9).toFixed(2) + ' GHz';
    if (hertz >= 1e6) return (hertz / 1e6).toFixed(0) + ' MHz';
    return hertz + ' Hz';
}

// Formata tempo de atividade em formato legivel (dias, horas, minutos)
function formatarTempoAtividade(segundos) {
    if (!segundos) return 'N/A';
    const dias = Math.floor(segundos / 86400);
    const horas = Math.floor((segundos % 86400) / 3600);
    const minutos = Math.floor((segundos % 3600) / 60);
    if (dias > 0) return `${dias}d ${horas}h ${minutos}m`;
    if (horas > 0) return `${horas}h ${minutos}m`;
    return `${minutos}m`;
}

// Mapeia o selo de classificacao para a classe CSS correspondente
function obterClasseSelo(selo) {
    if (!selo) return '';
    const seloMinusculo = selo.toLowerCase();
    if (seloMinusculo.includes('otimo') || seloMinusculo.includes('excelente')) return 'bom';
    if (seloMinusculo.includes('atencao') || seloMinusculo.includes('regular')) return 'atencao';
    if (seloMinusculo.includes('critico') || seloMinusculo.includes('lento')) return 'critico';
    return 'bom';
}

// ==================== NAVEGACAO SPA (APLICATIVO DE PAGINA UNICA) ====================
// Configura os links de navegacao para alternar entre paginas sem recarregar
document.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', (evento) => {
        evento.preventDefault();
        const pagina = link.dataset.page;

        // Remover classe 'active' de todos os links e paginas
        document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
        document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));

        // Ativar o link e a pagina clicada
        link.classList.add('active');
        document.getElementById(`page-${pagina}`).classList.add('active');

        // Carregar dados especificos de cada pagina ao acessar
        if (pagina === 'historico') carregarHistorico();
        if (pagina === 'config') carregarAlertas();
        if (pagina === 'dashboard') carregarUltimosTestes();
        if (pagina === 'hardware') carregarHardware();
    });
});

// Configura as abas de testes (Estresse e Disco)
document.querySelectorAll('.tab').forEach(aba => {
    aba.addEventListener('click', () => {
        document.querySelectorAll('.tab').forEach(a => a.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
        aba.classList.add('active');
        document.getElementById(`tab-${aba.dataset.tab}`).classList.add('active');
    });
});

// ==================== WEBSOCKET — COMUNICACAO EM TEMPO REAL ====================

// Estabelece conexao WebSocket com o servidor
function conectarWebSocket() {
    const protocolo = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const endereco = `${protocolo}//${location.host}/ws/monitoramento`;

    try {
        estado.conexaoWebSocket = new WebSocket(endereco);
    } catch (erro) {
        console.error('[GuidePC] Erro ao criar conexao WebSocket:', erro);
        agendarReconexao();
        return;
    }

    // Callback: conexao aberta com sucesso
    estado.conexaoWebSocket.onopen = () => {
        estado.tentativasReconexao = 0;
        document.getElementById('indicadorconexaoWebSocket').classList.add('connected');
        document.getElementById('textoStatusConexao').textContent = 'Conectado';
    };

    // Callback: conexao fechada (dispara reconexao)
    estado.conexaoWebSocket.onclose = () => {
        document.getElementById('indicadorconexaoWebSocket').classList.remove('connected');
        document.getElementById('textoStatusConexao').textContent = 'Desconectado';
        agendarReconexao();
    };

    // Callback: erro na conexao
    estado.conexaoWebSocket.onerror = () => {
        console.warn('[GuidePC] Erro na conexao WebSocket');
    };

    // Callback: mensagem recebida do servidor
    estado.conexaoWebSocket.onmessage = (evento) => {
        try {
            const dados = JSON.parse(evento.data);
            processarMensagemWebSocket(dados);
        } catch (erro) {
            console.warn('[GuidePC] Erro ao processar mensagem WebSocket:', erro);
        }
    };
}

// Agenda tentativa de reconexao com backoff exponencial
function agendarReconexao() {
    if (estado.tentativasReconexao >= estado.maxTentativasReconexao) {
        document.getElementById('textoStatusConexao').textContent = 'Falha na conexao';
        return;
    }

    // Tempo de espera aumenta exponencialmente: 1s, 2s, 4s, 8s... ate 30s
    const atraso = Math.min(1000 * Math.pow(2, estado.tentativasReconexao), 30000);
    estado.tentativasReconexao++;
    document.getElementById('textoStatusConexao').textContent = `Reconectando (${estado.tentativasReconexao})...`;

    if (estado.agendadorReconexao) clearTimeout(estado.agendadorReconexao);
    estado.agendadorReconexao = setTimeout(conectarWebSocket, atraso);
}

// Processa as mensagens recebidas via WebSocket
function processarMensagemWebSocket(dados) {
    switch (dados.tipo) {
        case 'METRICAS':
            atualizarPainel(dados);
            break;
        case 'PROGRESSO':
            atualizarProgressoTeste(dados.percentual);
            break;
        case 'AMOSTRA':
            adicionarAmostraTeste(dados);
            break;
        case 'RESULTADO':
            mostrarResultadoTeste(dados);
            break;
        case 'ALERTA':
            mostrarAlerta(dados.mensagem);
            break;
    }
}

// ==================== PAINEL DE MONITORAMENTO ====================

// Atualiza todos os cards do painel com os dados recebidos
function atualizarPainel(dados) {
    // --- CPU ---
    const usoCpu = dados.cpu.usoPercentual.toFixed(1);
    document.getElementById('valorCpu').textContent = `${usoCpu}%`;
    document.getElementById('barraCpu').style.width = `${usoCpu}%`;
    // Cor da barra varia conforme o nivel de uso
    document.getElementById('barraCpu').style.background = usoCpu > 85 ? '#f44336' : usoCpu > 60 ? '#ff9800' : '#C41E3A';
    document.getElementById('complementoCpu').textContent = `${dados.cpu.temperaturaCelsius > 0 ? dados.cpu.temperaturaCelsius.toFixed(1) + '\u00B0C' : 'N/A'}`;

    // --- Memoria RAM ---
    const usoRam = dados.memoria.percentualUso.toFixed(1);
    const ramDisponivel = (dados.memoria.disponivelBytes / (1024**3)).toFixed(1);
    const ramTotal = (dados.memoria.totalBytes / (1024**3)).toFixed(1);
    document.getElementById('valorRam').textContent = `${usoRam}%`;
    document.getElementById('barraRam').style.width = `${usoRam}%`;
    document.getElementById('complementoRam').textContent = `${ramDisponivel} / ${ramTotal} GB livres`;

    // --- Temperatura CPU ---
    const temperatura = dados.cpu.temperaturaCelsius;
    if (temperatura > 0) {
        document.getElementById('valorTemperatura').textContent = `${temperatura.toFixed(1)}\u00B0C`;
        document.getElementById('barraTemperatura').style.width = `${Math.min(100, temperatura)}%`;
        document.getElementById('barraTemperatura').style.background = temperatura > 85 ? '#f44336' : temperatura > 65 ? '#ff9800' : '#4caf50';
    } else {
        document.getElementById('valorTemperatura').textContent = 'N/A';
        document.getElementById('barraTemperatura').style.width = '0%';
    }
    document.getElementById('complementoTemperatura').textContent = temperatura > 0 ? (temperatura > 85 ? 'Critico!' : temperatura > 65 ? 'Quente' : 'Normal') : 'Sem sensor';

    // --- GPU ---
    const usoGpu = dados.gpu.usoPercentual;
    const temperaturaGpu = dados.gpu.temperaturaCelsius;
    if (!isNaN(usoGpu) && !isNaN(temperaturaGpu) && (usoGpu > 0 || temperaturaGpu > 0)) {
        document.getElementById('valorGpu').textContent = !isNaN(usoGpu) && usoGpu > 0 ? `${usoGpu.toFixed(1)}%` : 'N/A';
        document.getElementById('barraGpu').style.width = `${usoGpu}%`;
        document.getElementById('barraGpu').style.background = '#6c5ce7';
        document.getElementById('complementoGpu').textContent = !isNaN(temperaturaGpu) && temperaturaGpu > 0 ? `${temperaturaGpu.toFixed(1)}\u00B0C` : 'Sem temperatura';
    } else {
        document.getElementById('valorGpu').textContent = 'N/A';
        document.getElementById('complementoGpu').textContent = 'Nao detectada';
    }

    // --- Bateria (apenas se houver bateria no sistema) ---
    if (dados.bateria && dados.bateria.percentual >= 0) {
        document.getElementById('cardBateria').style.display = 'block';
        const bateria = dados.bateria;
        document.getElementById('valorBateria').textContent = `${bateria.percentual.toFixed(0)}%`;
        document.getElementById('barraBateria').style.width = `${bateria.percentual}%`;
        // Cor da barra de bateria: verde > 50%, amarelo > 20%, vermelho <= 20%
        const corBateria = bateria.percentual > 50 ? '#4caf50' : bateria.percentual > 20 ? '#ff9800' : '#f44336';
        document.getElementById('barraBateria').style.background = corBateria;
        let complemento = bateria.carregando ? 'Carregando' : 'Descarregando';
        if (bateria.tempoRestanteSegundos > 0) {
            const horas = Math.floor(bateria.tempoRestanteSegundos / 3600);
            const minutos = Math.floor((bateria.tempoRestanteSegundos % 3600) / 60);
            complemento += ` ~${horas}h${minutos}m`;
        }
        document.getElementById('complementoBateria').textContent = complemento;
    }

    // Adicionar ponto nos graficos
    adicionarPontoGrafico(usoCpu, usoRam, temperatura, usoGpu);
}

// ==================== GRAFICOS DE MONITORAMENTO ====================

// Cria configuracao padrao para um grafico de linha
function criarConfiguracaoGrafico(titulo, cor, maximo) {
    return {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 300 },
        scales: {
            y: {
                min: 0,
                max: maximo || 100,
                ticks: { color: '#888', callback: valor => valor + (titulo === 'temperatura' ? '\u00B0C' : '%') },
                grid: { color: '#333' }
            },
            x: {
                ticks: { color: '#888', maxTicksLimit: 10 },
                grid: { display: false }
            }
        },
        plugins: { legend: { display: false } }
    };
}

// Inicializa os quatro graficos de monitoramento
function inicializarGraficos() {
    estado.graficoCpu = new Chart(document.getElementById('graficoCpu').getContext('2d'), {
        type: 'line',
        data: { labels: [], datasets: [{ data: [], borderColor: '#C41E3A', backgroundColor: 'rgba(196,30,58,0.1)', fill: true, tension: 0.3, pointRadius: 0 }] },
        options: criarConfiguracaoGrafico('cpu', '#C41E3A')
    });

    estado.graficoRam = new Chart(document.getElementById('graficoRam').getContext('2d'), {
        type: 'line',
        data: { labels: [], datasets: [{ data: [], borderColor: '#4caf50', backgroundColor: 'rgba(76,175,80,0.1)', fill: true, tension: 0.3, pointRadius: 0 }] },
        options: criarConfiguracaoGrafico('ram', '#4caf50')
    });

    estado.graficoTemperatura = new Chart(document.getElementById('graficoTemperatura').getContext('2d'), {
        type: 'line',
        data: { labels: [], datasets: [{ data: [], borderColor: '#ff9800', backgroundColor: 'rgba(255,152,0,0.1)', fill: true, tension: 0.3, pointRadius: 0 }] },
        options: criarConfiguracaoGrafico('temperatura', '#ff9800', 120)
    });

    estado.graficoGpu = new Chart(document.getElementById('graficoGpu').getContext('2d'), {
        type: 'line',
        data: { labels: [], datasets: [{ data: [], borderColor: '#6c5ce7', backgroundColor: 'rgba(108,92,231,0.1)', fill: true, tension: 0.3, pointRadius: 0 }] },
        options: criarConfiguracaoGrafico('gpu', '#6c5ce7')
    });
}

// Adiciona um novo ponto de dados em todos os graficos
function adicionarPontoGrafico(cpu, ram, temperatura, gpu) {
    const agora = new Date();
    const rotulo = agora.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

    // Adicionar novos dados
    estado.rotulosTempo.push(rotulo);
    estado.dadosCpu.push(parseFloat(cpu));
    estado.dadosRam.push(parseFloat(ram));
    estado.dadosTemperatura.push(parseFloat(temperatura) || 0);
    estado.dadosGpu.push(parseFloat(gpu) || 0);

    // Manter no maximo 300 pontos (5 minutos a 1 ponto/segundo)
    if (estado.rotulosTempo.length > 300) {
        estado.rotulosTempo.shift();
        estado.dadosCpu.shift();
        estado.dadosRam.shift();
        estado.dadosTemperatura.shift();
        estado.dadosGpu.shift();
    }

    // Atualizar grafico de CPU
    estado.graficoCpu.data.labels = estado.rotulosTempo;
    estado.graficoCpu.data.datasets[0].data = estado.dadosCpu;
    estado.graficoCpu.update('none');

    // Atualizar grafico de RAM
    estado.graficoRam.data.labels = estado.rotulosTempo;
    estado.graficoRam.data.datasets[0].data = estado.dadosRam;
    estado.graficoRam.update('none');

    // Atualizar grafico de Temperatura
    estado.graficoTemperatura.data.labels = estado.rotulosTempo;
    estado.graficoTemperatura.data.datasets[0].data = estado.dadosTemperatura;
    estado.graficoTemperatura.update('none');

    // Atualizar grafico de GPU
    estado.graficoGpu.data.labels = estado.rotulosTempo;
    estado.graficoGpu.data.datasets[0].data = estado.dadosGpu;
    estado.graficoGpu.update('none');
}

// ==================== PAGINA DE HARDWARE ====================

// Carrega e exibe informacoes detalhadas do hardware do sistema
async function carregarHardware() {
    try {
        const resposta = await fetch('/api/hardware');
        if (!resposta.ok) throw new Error(`Erro HTTP ${resposta.status}`);
        const hardware = await resposta.json();

        const container = document.getElementById('containerHardware');
        container.textContent = '';

        // Definir todas as secoes de hardware a serem exibidas
        const secoes = [
            {
                titulo: 'Processador',
                itens: [
                    ['Fabricante', hardware.processador.fabricante],
                    ['Modelo', hardware.processador.modelo],
                    ['Microarquitetura', hardware.processador.microarquitetura],
                    ['Nucleos Fisicos', hardware.processador.nucleosFisicos],
                    ['Nucleos Logicos', hardware.processador.nucleosLogicos],
                    ['Frequencia Base', formatarHertz(hardware.processador.frequenciaBaseHz)],
                    ['Frequencia Maxima', formatarHertz(hardware.processador.frequenciaMaximaHz)],
                    ['Uso Atual', hardware.processador.percentualUso.toFixed(1) + '%'],
                    ['Temperatura', hardware.processador.temperaturaCelsius > 0 ? hardware.processador.temperaturaCelsius.toFixed(1) + '\u00B0C' : 'N/A']
                ]
            },
            {
                titulo: 'Memoria RAM',
                itens: [
                    ['Total', formatarBytes(hardware.memoria.totalBytes)],
                    ['Disponivel', formatarBytes(hardware.memoria.disponivelBytes)],
                    ['Em Uso', formatarBytes(hardware.memoria.emUsoBytes)],
                    ['Percentual', hardware.memoria.percentualUso.toFixed(1) + '%'],
                    ['Pagina', hardware.memoria.tamanhoPaginaBytes + ' bytes']
                ]
            },
            {
                titulo: 'Armazenamento',
                itens: hardware.discos.map(disco => [disco.nome + ' (' + disco.tipoInferido + ')', formatarBytes(disco.tamanhoBytes)])
            },
            {
                titulo: 'Placa Mae',
                itens: [
                    ['Fabricante', hardware.placaMae.fabricante],
                    ['Modelo', hardware.placaMae.modelo],
                    ['Versao BIOS', hardware.placaMae.versaoBios]
                ]
            },
            {
                titulo: 'Sistema Operacional',
                itens: [
                    ['Familia', hardware.sistemaOperacional.familia],
                    ['Versao', hardware.sistemaOperacional.versao],
                    ['Arquitetura', hardware.sistemaOperacional.arquitetura],
                    ['Uptime', formatarTempoAtividade(hardware.sistemaOperacional.tempoAtividadeSegundos)]
                ]
            },
            {
                titulo: 'GPU',
                itens: hardware.gpus.map(gpu => ['GPU', gpu])
            }
        ];

        // Criar e inserir cada secao no DOM
        for (const secao of secoes) {
            const divSecao = document.createElement('div');
            divSecao.className = 'hardware-section card';

            const titulo = document.createElement('h3');
            titulo.textContent = secao.titulo;
            divSecao.appendChild(titulo);

            const grade = document.createElement('div');
            grade.className = 'hardware-grid';

            for (const [rotulo, valor] of secao.itens) {
                const item = document.createElement('div');
                item.className = 'hardware-item';
                item.innerHTML = `<span class="label">${escaparHtml(rotulo)}</span><span class="valor">${escaparHtml(valor)}</span>`;
                grade.appendChild(item);
            }

            divSecao.appendChild(grade);
            container.appendChild(divSecao);
        }

        // Carregar espaco em disco apos carregar hardware
        carregarEspacoDisco();
    } catch (erro) {
        console.error('[GuidePC] Erro ao carregar informacoes de hardware:', erro);
        document.getElementById('containerHardware').innerHTML = '<p style="color:var(--cor-texto-secundario)">Erro ao carregar informacoes de hardware</p>';
    }
}

// ==================== ESPACO EM DISCO ====================

// Carrega e exibe o espaco utilizado por cada particao de disco
async function carregarEspacoDisco() {
    try {
        const resposta = await fetch('/api/hardware/disco-espaco');
        if (!resposta.ok) return;
        const particoes = await resposta.json();
        if (!particoes || particoes.length === 0) return;

        const card = document.getElementById('cardDiscoEspaco');
        card.style.display = 'block';
        const container = document.getElementById('containerDiscoEspaco');
        container.textContent = '';

        for (const particao of particoes) {
            if (particao.totalBytes === 0) continue;
            const percentual = ((particao.usadoBytes / particao.totalBytes) * 100).toFixed(1);
            // Classe CSS baseada no nivel de ocupacao: ok < 70%, aviso < 90%, critico >= 90%
            const classe = percentual > 90 ? 'critico' : percentual > 70 ? 'aviso' : 'ok';

            const divParticao = document.createElement('div');
            divParticao.className = 'disco-particao';
            divParticao.innerHTML = `
                <div class="disco-label">
                    <span>${escaparHtml(particao.pontoMontagem)}</span>
                    <span>${formatarBytes(particao.usadoBytes)} / ${formatarBytes(particao.totalBytes)} (${percentual}%)</span>
                </div>
                <div class="disco-bar">
                    <div class="disco-bar-fill ${classe}" style="width: ${percentual}%"></div>
                </div>
            `;
            container.appendChild(divParticao);
        }
    } catch (erro) {
        console.warn('[GuidePC] Erro ao carregar espaco em disco:', erro);
    }
}

// ==================== TESTES DE DESEMPENHO ====================

// Iniciar teste de estresse (CPU/RAM)
document.getElementById('botaoIniciarEstresse').addEventListener('click', async () => {
    const nivel = document.getElementById('nivelEstresse').value;
    const duracao = parseInt(document.getElementById('duracaoEstresse').value);

    if (isNaN(duracao) || duracao < 5 || duracao > 600) {
        mostrarAlerta('Duracao invalida (minimo 5, maximo 600 segundos)');
        return;
    }

    try {
        const resposta = await fetch('/api/teste/estresse', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nivel, duracao })
        });
        const dados = await resposta.json();
        if (resposta.ok) mostrarProgressoTeste();
        else mostrarAlerta(dados.erro || 'Erro ao iniciar teste de estresse');
    } catch (erro) {
        mostrarAlerta('Erro de conexao ao iniciar teste');
    }
});

// Iniciar teste de disco (leitura/escrita)
document.getElementById('botaoIniciarDisco').addEventListener('click', async () => {
    const tipo = document.getElementById('tipoDisco').value;
    const duracao = parseInt(document.getElementById('duracaoDisco').value);

    if (isNaN(duracao) || duracao < 5 || duracao > 120) {
        mostrarAlerta('Duracao invalida (minimo 5, maximo 120 segundos)');
        return;
    }

    try {
        const resposta = await fetch('/api/teste/disco', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tipo, duracao })
        });
        const dados = await resposta.json();
        if (resposta.ok) mostrarProgressoTeste();
        else mostrarAlerta(dados.erro || 'Erro ao iniciar teste de disco');
    } catch (erro) {
        mostrarAlerta('Erro de conexao ao iniciar teste');
    }
});

// Parar teste em execucao
document.getElementById('botaoPararTeste').addEventListener('click', async () => {
    if (!confirm('Tem certeza que deseja parar o teste?')) return;
    try {
        const resposta = await fetch('/api/teste/parar', { method: 'POST' });
        if (!resposta.ok) mostrarAlerta('Erro ao parar teste');
    } catch (erro) {
        mostrarAlerta('Erro de conexao ao parar teste');
    }
});

// Exibir card de progresso do teste
function mostrarProgressoTeste() {
    document.getElementById('cardProgressoTeste').style.display = 'block';
    document.getElementById('cardResultadoTeste').style.display = 'none';
    document.getElementById('barraProgressoTeste').style.width = '0%';
    document.getElementById('textoProgressoTeste').textContent = '0%';
    document.getElementById('metricasTeste').textContent = '';
}

// Atualizar barra de progresso do teste
function atualizarProgressoTeste(percentual) {
    document.getElementById('barraProgressoTeste').style.width = `${percentual}%`;
    document.getElementById('textoProgressoTeste').textContent = `${percentual}%`;
}

// Adicionar amostra de metricas durante o teste
function adicionarAmostraTeste(amostra) {
    const metricas = document.getElementById('metricasTeste');
    metricas.textContent = '';

    const itensMetrica = [
        `CPU: ${amostra.cargaCpu.toFixed(1)}%`,
        `RAM: ${amostra.usoMemoria.toFixed(1)}%`,
        `Temperatura: ${amostra.temperatura > 0 ? amostra.temperatura.toFixed(1) + '\u00B0C' : 'N/A'}`,
        `Resposta: ${amostra.tempoRespostaMs.toFixed(1)}ms`
    ];

    for (const item of itensMetrica) {
        const divMetrica = document.createElement('div');
        divMetrica.textContent = item;
        metricas.appendChild(divMetrica);
    }
}

// Exibir resultado final do teste
function mostrarResultadoTeste(dados) {
    document.getElementById('cardProgressoTeste').style.display = 'none';
    document.getElementById('cardResultadoTeste').style.display = 'block';

    const container = document.getElementById('containerResultado');
    container.textContent = '';

    const divMetricas = document.createElement('div');
    divMetricas.className = 'teste-metrics';

    // Montar lista de metricas conforme o tipo de teste
    const itensResultado = dados.tipo === 'ESTRESSE' ? [
        `Nivel: ${dados.nivel}`,
        `CPU Media: ${dados.mediaCpu.toFixed(1)}%`,
        `CPU Pico: ${dados.maxCpu.toFixed(1)}%`,
        `Duracao: ${dados.duracao}s`
    ] : [
        `Tipo: ${dados.tipoTeste}`,
        `Throughput: ${dados.throughputMbps.toFixed(2)} MB/s`,
        `IOPS: ${dados.iops.toFixed(0)}`,
        `Duracao: ${dados.duracao}s`
    ];

    for (const item of itensResultado) {
        const divItem = document.createElement('div');
        divItem.textContent = item;
        divMetricas.appendChild(divItem);
    }

    // Exibir selo de classificacao
    const divSelo = document.createElement('div');
    const strongSelo = document.createElement('strong');
    strongSelo.className = `selo selo-${obterClasseSelo(dados.selo)}`;
    strongSelo.textContent = dados.selo;
    divSelo.textContent = 'Selo: ';
    divSelo.appendChild(strongSelo);
    divMetricas.appendChild(divSelo);

    container.appendChild(divMetricas);
}

// ==================== HISTORICO DE TESTES ====================

// Carrega a lista de testes executados com filtros e paginacao
async function carregarHistorico(pagina = 1) {
    const tipoFiltro = document.getElementById('filtroTipo').value;
    const nivelFiltro = document.getElementById('filtroNivel').value;
    const parametros = new URLSearchParams({ pagina, tamanhoPagina: 15 });
    if (tipoFiltro) parametros.append('tipo', tipoFiltro);
    if (nivelFiltro) parametros.append('nivel', nivelFiltro);

    try {
        const resposta = await fetch(`/api/historico?${parametros}`);
        if (!resposta.ok) throw new Error(`Erro HTTP ${resposta.status}`);
        const dados = await resposta.json();

        const container = document.getElementById('tabelaHistorico');
        container.textContent = '';

        if (!dados.testes || dados.testes.length === 0) {
            const paragrafoVazio = document.createElement('p');
            paragrafoVazio.style.color = 'var(--cor-texto-secundario)';
            paragrafoVazio.textContent = 'Nenhum teste encontrado';
            container.appendChild(paragrafoVazio);
            estado.paginaAtual = 1;
            estado.totalPaginas = 1;
            renderizarPaginacao();
            return;
        }

        // Criar tabela de historico
        const tabela = document.createElement('table');

        // Cabecalho da tabela
        const cabecalho = document.createElement('thead');
        const linhaCabecalho = document.createElement('tr');
        for (const tituloColuna of ['ID', 'Tipo', 'Nivel', 'Duracao', 'CPU Media', 'Selo', 'Data', 'Acoes']) {
            const celulaCabecalho = document.createElement('th');
            celulaCabecalho.textContent = tituloColuna;
            linhaCabecalho.appendChild(celulaCabecalho);
        }
        cabecalho.appendChild(linhaCabecalho);
        tabela.appendChild(cabecalho);

        // Corpo da tabela com os testes
        const corpo = document.createElement('tbody');
        for (const teste of dados.testes) {
            const linha = document.createElement('tr');
            for (const valor of [
                teste.id,
                teste.tipo,
                teste.nivel || teste.tipoDisco || '',
                teste.duracaoSegundos + 's',
                teste.tipo === 'ESTRESSE' ? teste.mediaCpu.toFixed(1) + '%' : teste.throughputMbps.toFixed(1) + ' MB/s',
                teste.selo || '-',
                teste.inicio
            ]) {
                const celula = document.createElement('td');
                celula.textContent = valor;
                linha.appendChild(celula);
            }

            // Botes de acao (Detalhes e Excluir)
            const celulaAcoes = document.createElement('td');
            celulaAcoes.className = 'td-acoes';

            const botaoDetalhe = document.createElement('button');
            botaoDetalhe.className = 'btn btn-secondary';
            botaoDetalhe.textContent = 'Detalhes';
            botaoDetalhe.style.marginRight = '4px';
            botaoDetalhe.addEventListener('click', () => abrirDetalhe(teste.id, teste.tipo));
            celulaAcoes.appendChild(botaoDetalhe);

            const botaoExcluir = document.createElement('button');
            botaoExcluir.className = 'btn btn-secondary';
            botaoExcluir.textContent = 'Excluir';
            botaoExcluir.addEventListener('click', () => deletarTeste(teste.id));
            celulaAcoes.appendChild(botaoExcluir);

            linha.appendChild(celulaAcoes);
            corpo.appendChild(linha);
        }
        tabela.appendChild(corpo);
        container.appendChild(tabela);

        // Atualizar paginacao
        estado.paginaAtual = dados.pagina;
        estado.totalPaginas = dados.totalPaginas;
        renderizarPaginacao();
    } catch (erro) {
        console.error('[GuidePC] Erro ao carregar historico:', erro);
        document.getElementById('tabelaHistorico').innerHTML = '<p style="color:var(--cor-texto-secundario)">Erro ao carregar historico</p>';
    }
}

// Renderiza os botoes de paginacao
function renderizarPaginacao() {
    const container = document.getElementById('paginacaoHistorico');
    container.textContent = '';
    for (let i = 1; i <= estado.totalPaginas; i++) {
        const botaoPagina = document.createElement('button');
        botaoPagina.className = i === estado.paginaAtual ? 'active' : '';
        botaoPagina.textContent = i;
        botaoPagina.addEventListener('click', () => carregarHistorico(i));
        container.appendChild(botaoPagina);
    }
}

// Deleta um teste do historico
async function deletarTeste(id) {
    if (!confirm('Deletar este teste?')) return;
    try {
        const resposta = await fetch(`/api/historico/${id}`, { method: 'DELETE' });
        if (resposta.ok) carregarHistorico(estado.paginaAtual);
        else mostrarAlerta('Erro ao deletar teste');
    } catch (erro) {
        mostrarAlerta('Erro ao deletar teste');
    }
}

// Botao de filtrar historico
document.getElementById('botaoFiltrar').addEventListener('click', () => carregarHistorico(1));

// ==================== MODAL DE DETALHES DO TESTE ====================

// Abre o modal com grafico detalhado das amostras do teste
async function abrirDetalhe(id, tipo) {
    try {
        const resposta = await fetch(`/api/historico/${id}/amostras`);
        if (!resposta.ok) throw new Error(`Erro HTTP ${resposta.status}`);
        const amostras = await resposta.json();

        // Exibir o modal
        document.getElementById('modalDetalhe').style.display = 'flex';

        // Destruir grafico anterior se existir
        if (estado.instanciaGraficoDetalhe) {
            estado.instanciaGraficoDetalhe.destroy();
        }

        // Criar rotulos de tempo (a cada 500ms = 2 amostras por segundo)
        const rotulos = amostras.map((amostra, indice) => {
            const segundos = Math.floor(indice / 2);
            return segundos + 's';
        });

        // Configurar datasets do grafico
        const conjuntosDados = [
            { label: 'CPU %', data: amostras.map(a => a.cargaCpu), borderColor: '#C41E3A', tension: 0.3, pointRadius: 0 },
            { label: 'RAM %', data: amostras.map(a => a.usoMemoria), borderColor: '#4caf50', tension: 0.3, pointRadius: 0 }
        ];

        // Adicionar eixo de temperatura apenas para testes de estresse
        if (tipo === 'ESTRESSE') {
            conjuntosDados.push({ label: 'Temperatura \u00B0C', data: amostras.map(a => a.temperatura), borderColor: '#ff9800', tension: 0.3, pointRadius: 0, yAxisID: 'y1' });
        }

        // Criar o grafico de detalhe
        estado.instanciaGraficoDetalhe = new Chart(document.getElementById('graficoDetalhe').getContext('2d'), {
            type: 'line',
            data: { labels: rotulos, datasets: conjuntosDados },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: { min: 0, max: 100, ticks: { color: '#888' }, grid: { color: '#333' } },
                    y1: tipo === 'ESTRESSE' ? { position: 'right', min: 0, max: 120, ticks: { color: '#ff9800' }, grid: { display: false } } : undefined,
                    x: { ticks: { color: '#888', maxTicksLimit: 15 }, grid: { display: false } }
                },
                plugins: { legend: { labels: { color: '#ccc' } } }
            }
        });

        // Exibir informacoes complementares
        const informacoes = document.getElementById('informacoesDetalhe');
        informacoes.textContent = `${amostras.length} amostras coletadas (500ms cada)`;
    } catch (erro) {
        console.error('[GuidePC] Erro ao carregar detalhes do teste:', erro);
        mostrarAlerta('Erro ao carregar detalhes do teste');
    }
}

// Fechar o modal de detalhes
document.getElementById('botaoFecharModal').addEventListener('click', () => {
    document.getElementById('modalDetalhe').style.display = 'none';
});

// Fechar modal ao clicar fora do conteudo
document.getElementById('modalDetalhe').addEventListener('click', (evento) => {
    if (evento.target === document.getElementById('modalDetalhe')) {
        document.getElementById('modalDetalhe').style.display = 'none';
    }
});

// ==================== CONFIGURACOES ====================

// Carrega as configuracoes de alerta do servidor
async function carregarAlertas() {
    try {
        const resposta = await fetch('/api/teste/alertas');
        if (!resposta.ok) throw new Error(`Erro HTTP ${resposta.status}`);
        const dados = await resposta.json();
        document.getElementById('habilitarAlertas').checked = dados.habilitado;
        document.getElementById('limiteCpu').value = dados.limiteCpu;
        document.getElementById('valorLimiteCpu').textContent = dados.limiteCpu + '%';
        document.getElementById('limiteTemperatura').value = dados.limiteTemperatura;
        document.getElementById('valorLimiteTemperatura').textContent = dados.limiteTemperatura + '\u00B0C';
        document.getElementById('limiteMemoria').value = dados.limiteMemoria;
        document.getElementById('valorLimiteMemoria').textContent = dados.limiteMemoria + '%';
    } catch (erro) {
        console.error('[GuidePC] Erro ao carregar configuracoes de alerta:', erro);
    }
}

// Atualizar valor exibido ao mover os controles deslizantes
['limiteCpu', 'limiteTemperatura', 'limiteMemoria'].forEach(idControle => {
    document.getElementById(idControle).addEventListener('input', (evento) => {
        const sufixo = idControle === 'limiteTemperatura' ? '\u00B0C' : '%';
        const rotuloValor = idControle === 'limiteCpu' ? 'valorLimiteCpu' : idControle === 'limiteTemperatura' ? 'valorLimiteTemperatura' : 'valorLimiteMemoria';
        document.getElementById(rotuloValor).textContent = evento.target.value + sufixo;
    });
});

// Salvar configuracoes de alerta no servidor
document.getElementById('botaoSalvarAlertas').addEventListener('click', async () => {
    const configuracoes = {
        habilitado: document.getElementById('habilitarAlertas').checked,
        limiteCpu: parseFloat(document.getElementById('limiteCpu').value),
        limiteTemperatura: parseFloat(document.getElementById('limiteTemperatura').value),
        limiteMemoria: parseFloat(document.getElementById('limiteMemoria').value)
    };
    try {
        await fetch('/api/teste/alertas', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(configuracoes)
        });
        mostrarAlerta('Configuracoes salvas com sucesso!', 'sucesso');
    } catch (erro) {
        mostrarAlerta('Erro ao salvar configuracoes');
    }
});

// Botoes de exportacao de dados
document.getElementById('botaoExportarCsv').addEventListener('click', () => {
    window.location.href = '/api/relatorio/csv';
});
document.getElementById('botaoExportarPdf').addEventListener('click', () => {
    window.location.href = '/api/relatorio/pdf';
});

// ==================== ULTIMOS TESTES (DASHBOARD) ====================

// Carrega os 5 testes mais recentes para exibir no painel
async function carregarUltimosTestes() {
    try {
        const resposta = await fetch('/api/historico?tamanhoPagina=5');
        if (!resposta.ok) throw new Error(`Erro HTTP ${resposta.status}`);
        const dados = await resposta.json();
        const container = document.getElementById('ultimosTestes');
        container.textContent = '';

        if (!dados.testes || dados.testes.length === 0) {
            const paragrafoVazio = document.createElement('p');
            paragrafoVazio.style.color = 'var(--cor-texto-secundario)';
            paragrafoVazio.textContent = 'Nenhum teste executado ainda';
            container.appendChild(paragrafoVazio);
            return;
        }

        // Criar tabela com os ultimos testes
        const tabela = document.createElement('table');

        const cabecalho = document.createElement('thead');
        const linhaCabecalho = document.createElement('tr');
        for (const tituloColuna of ['Tipo', 'Nivel', 'Duracao', 'Metrica', 'Selo', 'Data']) {
            const celulaCabecalho = document.createElement('th');
            celulaCabecalho.textContent = tituloColuna;
            linhaCabecalho.appendChild(celulaCabecalho);
        }
        cabecalho.appendChild(linhaCabecalho);
        tabela.appendChild(cabecalho);

        const corpo = document.createElement('tbody');
        for (const teste of dados.testes) {
            const linha = document.createElement('tr');
            for (const valor of [
                teste.tipo,
                teste.nivel || teste.tipoDisco || '',
                teste.duracaoSegundos + 's',
                teste.tipo === 'ESTRESSE' ? teste.mediaCpu.toFixed(1) + '% CPU' : teste.throughputMbps.toFixed(1) + ' MB/s',
                teste.selo || '-',
                teste.inicio
            ]) {
                const celula = document.createElement('td');
                celula.textContent = valor;
                linha.appendChild(celula);
            }
            corpo.appendChild(linha);
        }
        tabela.appendChild(corpo);
        container.appendChild(tabela);
    } catch (erro) {
        console.error('[GuidePC] Erro ao carregar ultimos testes:', erro);
    }
}

// ==================== SISTEMA DE ALERTAS TOAST ====================

// Exibe uma notificacao temporal (toast) na tela
function mostrarAlerta(mensagem, tipo = 'erro') {
    const container = document.getElementById('containerAlertas');
    const notificacao = document.createElement('div');
    notificacao.className = 'alerta-toast';
    // Cor do toast: verde para sucesso, vermelho para erro
    notificacao.style.background = tipo === 'sucesso' ? 'var(--cor-sucesso)' : 'var(--cor-perigo)';
    notificacao.textContent = mensagem;
    container.appendChild(notificacao);
    // Remover apos 5.5 segundos
    setTimeout(() => { if (notificacao.parentNode) notificacao.remove(); }, 5500);
}

// ==================== INICIALIZACAO ====================

// Quando o DOM estiver completamente carregado, inicializar o aplicativo
document.addEventListener('DOMContentLoaded', () => {
    inicializarGraficos();
    conectarWebSocket();
    carregarUltimosTestes();
});
