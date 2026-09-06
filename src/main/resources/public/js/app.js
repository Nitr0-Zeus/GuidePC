// ==================== GUIDEPC v3.0 — Frontend ====================

// ==================== ESTADO GLOBAL ====================
const state = {
    ws: null,
    chartCpu: null,
    chartRam: null,
    cpuData: [],
    ramData: [],
    timeLabels: [],
    currentPage: 1,
    totalPaginas: 1
};

// ==================== NAVEGACAO SPA ====================
document.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        const page = link.dataset.page;
        document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
        document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
        link.classList.add('active');
        document.getElementById(`page-${page}`).classList.add('active');

        if (page === 'historico') carregarHistorico();
        if (page === 'config') carregarAlertas();
        if (page === 'dashboard') carregarUltimosTestes();
    });
});

// Tabs nos testes
document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
        tab.classList.add('active');
        document.getElementById(`tab-${tab.dataset.tab}`).classList.add('active');
    });
});

// ==================== WEBSOCKET ====================
function conectarWebSocket() {
    const protocolo = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const url = `${protocolo}//${location.host}/ws/monitoramento`;

    state.ws = new WebSocket(url);

    state.ws.onopen = () => {
        document.getElementById('wsStatus').classList.add('connected');
        document.getElementById('wsStatusText').textContent = 'Conectado';
    };

    state.ws.onclose = () => {
        document.getElementById('wsStatus').classList.remove('connected');
        document.getElementById('wsStatusText').textContent = 'Reconectando...';
        setTimeout(conectarWebSocket, 3000);
    };

    state.ws.onmessage = (event) => {
        try {
            const dados = JSON.parse(event.data);
            processarMensagem(dados);
        } catch (e) {}
    };
}

function processarMensagem(dados) {
    switch (dados.tipo) {
        case 'METRICAS':
            atualizarDashboard(dados);
            break;
        case 'PROGRESSO':
            atualizarProgresso(dados.percentual);
            break;
        case 'AMOSTRA':
            adicionarAmostraTeste(dados);
            break;
        case 'RESULTADO':
            mostrarResultado(dados);
            break;
        case 'ALERTA':
            mostrarAlerta(dados.mensagem);
            break;
    }
}

// ==================== DASHBOARD ====================
function atualizarDashboard(dados) {
    // CPU
    const cpuUso = dados.cpu.usoPercentual.toFixed(1);
    document.getElementById('cpuValue').textContent = `${cpuUso}%`;
    document.getElementById('cpuBar').style.width = `${cpuUso}%`;
    document.getElementById('cpuBar').style.background = cpuUso > 85 ? '#f44336' : cpuUso > 60 ? '#ff9800' : '#8B0000';
    document.getElementById('cpuSub').textContent = `${dados.cpu.temperaturaCelsius > 0 ? dados.cpu.temperaturaCelsius.toFixed(1) + '°C' : 'N/A'}`;

    // RAM
    const ramUso = dados.memoria.percentualUso.toFixed(1);
    const ramGB = (dados.memoria.disponivelBytes / (1024**3)).toFixed(1);
    const ramTotal = (dados.memoria.totalBytes / (1024**3)).toFixed(1);
    document.getElementById('ramValue').textContent = `${ramUso}%`;
    document.getElementById('ramBar').style.width = `${ramUso}%`;
    document.getElementById('ramSub').textContent = `${ramGB} / ${ramTotal} GB livres`;

    // Temperatura
    const temp = dados.cpu.temperaturaCelsius;
    if (temp > 0) {
        document.getElementById('tempValue').textContent = `${temp.toFixed(1)}°C`;
        document.getElementById('tempBar').style.width = `${Math.min(100, temp)}%`;
        document.getElementById('tempBar').style.background = temp > 85 ? '#f44336' : temp > 65 ? '#ff9800' : '#4caf50';
    } else {
        document.getElementById('tempValue').textContent = 'N/A';
        document.getElementById('tempBar').style.width = '0%';
    }
    document.getElementById('tempSub').textContent = temp > 0 ? (temp > 85 ? 'Crítico!' : temp > 65 ? 'Quente' : 'Normal') : 'Sem sensor';

    // GPU
    const gpuUso = dados.gpu.usoPercentual;
    const gpuTemp = dados.gpu.temperaturaCelsius;
    if (gpuUso > 0 || gpuTemp > 0) {
        document.getElementById('gpuValue').textContent = gpuUso > 0 ? `${gpuUso.toFixed(1)}%` : 'N/A';
        document.getElementById('gpuBar').style.width = `${gpuUso}%`;
        document.getElementById('gpuBar').style.background = '#6c5ce7';
        document.getElementById('gpuSub').textContent = gpuTemp > 0 ? `${gpuTemp.toFixed(1)}°C` : 'Sem temp';
    } else {
        document.getElementById('gpuValue').textContent = 'N/A';
        document.getElementById('gpuSub').textContent = 'Não detectada';
    }

    // Gráficos
    adicionarPontoGrafico(cpuUso, ramUso);
}

// ==================== GRAFICOS ====================
function inicializarGraficos() {
    const configBase = {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 300 },
        scales: {
            y: { min: 0, max: 100, ticks: { color: '#888', callback: v => v + '%' }, grid: { color: '#333' } },
            x: { ticks: { color: '#888', maxTicksLimit: 10 }, grid: { display: false } }
        },
        plugins: { legend: { display: false } }
    };

    const cpuCtx = document.getElementById('chartCpu').getContext('2d');
    state.chartCpu = new Chart(cpuCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                data: [],
                borderColor: '#8B0000',
                backgroundColor: 'rgba(139,0,0,0.1)',
                fill: true,
                tension: 0.3,
                pointRadius: 0
            }]
        },
        options: { ...configBase }
    });

    const ramCtx = document.getElementById('chartRam').getContext('2d');
    state.chartRam = new Chart(ramCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                data: [],
                borderColor: '#4caf50',
                backgroundColor: 'rgba(76,175,80,0.1)',
                fill: true,
                tension: 0.3,
                pointRadius: 0
            }]
        },
        options: { ...configBase }
    });
}

function adicionarPontoGrafico(cpu, ram) {
    const agora = new Date();
    const label = agora.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

    state.timeLabels.push(label);
    state.cpuData.push(parseFloat(cpu));
    state.ramData.push(parseFloat(ram));

    // Manter apenas últimos 300 pontos (5 min a cada 1s)
    if (state.timeLabels.length > 300) {
        state.timeLabels.shift();
        state.cpuData.shift();
        state.ramData.shift();
    }

    state.chartCpu.data.labels = [...state.timeLabels];
    state.chartCpu.data.datasets[0].data = [...state.cpuData];
    state.chartCpu.update('none');

    state.chartRam.data.labels = [...state.timeLabels];
    state.chartRam.data.datasets[0].data = [...state.ramData];
    state.chartRam.update('none');
}

// ==================== TESTES ====================
document.getElementById('btnIniciarEstresse').addEventListener('click', async () => {
    const nivel = document.getElementById('nivelEstresse').value;
    const duracao = parseInt(document.getElementById('duracaoEstresse').value);

    try {
        const resp = await fetch('/api/teste/estresse', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nivel, duracao })
        });
        const dados = await resp.json();
        if (resp.ok) {
            mostrarProgresso();
        } else {
            mostrarAlerta(dados.erro || 'Erro ao iniciar teste');
        }
    } catch (e) {
        mostrarAlerta('Erro de conexão');
    }
});

document.getElementById('btnIniciarDisco').addEventListener('click', async () => {
    const tipo = document.getElementById('tipoDisco').value;
    const duracao = parseInt(document.getElementById('duracaoDisco').value);

    try {
        const resp = await fetch('/api/teste/disco', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tipo, duracao })
        });
        const dados = await resp.json();
        if (resp.ok) {
            mostrarProgresso();
        } else {
            mostrarAlerta(dados.erro || 'Erro ao iniciar teste');
        }
    } catch (e) {
        mostrarAlerta('Erro de conexão');
    }
});

document.getElementById('btnParar').addEventListener('click', async () => {
    await fetch('/api/teste/parar', { method: 'POST' });
});

function mostrarProgresso() {
    document.getElementById('testeProgresso').style.display = 'block';
    document.getElementById('testeResultado').style.display = 'none';
    document.getElementById('progressFill').style.width = '0%';
    document.getElementById('progressText').textContent = '0%';
    document.getElementById('testeMetrics').innerHTML = '';
}

function atualizarProgresso(percentual) {
    document.getElementById('progressFill').style.width = `${percentual}%`;
    document.getElementById('progressText').textContent = `${percentual}%`;
}

function adicionarAmostraTeste(amostra) {
    const metrics = document.getElementById('testeMetrics');
    metrics.innerHTML = `
        <div>CPU: <strong>${amostra.cargaCpu.toFixed(1)}%</strong></div>
        <div>RAM: <strong>${amostra.usoMemoria.toFixed(1)}%</strong></div>
        <div>Temp: <strong>${amostra.temperatura > 0 ? amostra.temperatura.toFixed(1) + '°C' : 'N/A'}</strong></div>
        <div>Resp: <strong>${amostra.tempoRespostaMs.toFixed(1)}ms</strong></div>
    `;
}

function mostrarResultado(dados) {
    document.getElementById('testeProgresso').style.display = 'none';
    document.getElementById('testeResultado').style.display = 'block';

    let html = '<div class="teste-metrics">';
    if (dados.tipo === 'ESTRESSE') {
        html += `
            <div>Nível: <strong>${dados.nivel}</strong></div>
            <div>CPU Média: <strong>${dados.mediaCpu.toFixed(1)}%</strong></div>
            <div>CPU Pico: <strong>${dados.maxCpu.toFixed(1)}%</strong></div>
            <div>Selo: <strong class="selo selo-${seloClasse(dados.selo)}">${dados.selo}</strong></div>
            <div>Duração: <strong>${dados.duracao}s</strong></div>
        `;
    } else {
        html += `
            <div>Tipo: <strong>${dados.tipoTeste}</strong></div>
            <div>Throughput: <strong>${dados.throughputMbps.toFixed(2)} MB/s</strong></div>
            <div>IOPS: <strong>${dados.iops.toFixed(0)}</strong></div>
            <div>Selo: <strong class="selo selo-${seloClasse(dados.selo)}">${dados.selo}</strong></div>
            <div>Duração: <strong>${dados.duracao}s</strong></div>
        `;
    }
    html += '</div>';
    document.getElementById('resultadoConteudo').innerHTML = html;
}

function seloClasse(selo) {
    if (!selo) return '';
    const s = selo.toLowerCase();
    if (s.includes('ótimo') || s.includes('otimo') || s.includes('excelente')) return 'bom';
    if (s.includes('atenção') || s.includes('atencao') || s.includes('regular')) return 'atencao';
    if (s.includes('crítico') || s.includes('critico') || s.includes('lento')) return 'critico';
    return 'bom';
}

// ==================== HISTORICO ====================
async function carregarHistorico(pagina = 1) {
    const tipo = document.getElementById('filtroTipo').value;
    const nivel = document.getElementById('filtroNivel').value;

    const params = new URLSearchParams({ pagina, tamanhoPagina: 15 });
    if (tipo) params.append('tipo', tipo);
    if (nivel) params.append('nivel', nivel);

    try {
        const resp = await fetch(`/api/historico?${params}`);
        const dados = await resp.json();

        let html = '<table><thead><tr>';
        html += '<th>ID</th><th>Tipo</th><th>Nível</th><th>Duração</th><th>CPU Média</th><th>Selo</th><th>Data</th><th>Ações</th>';
        html += '</tr></thead><tbody>';

        if (dados.testes.length === 0) {
            html += '<tr><td colspan="8" style="text-align:center;color:var(--text-secondary)">Nenhum teste encontrado</td></tr>';
        }

        for (const t of dados.testes) {
            html += `<tr>
                <td>${t.id}</td>
                <td>${t.tipo}</td>
                <td>${t.nivel || t.tipoDisco || ''}</td>
                <td>${t.duracaoSegundos}s</td>
                <td>${t.tipo === 'ESTRESSE' ? t.mediaCpu.toFixed(1) + '%' : t.throughputMbps.toFixed(1) + ' MB/s'}</td>
                <td><span class="selo selo-${seloClasse(t.selo)}">${t.selo || '-'}</span></td>
                <td>${t.inicio}</td>
                <td><button class="btn btn-secondary" onclick="deletarTeste(${t.id})">🗑</button></td>
            </tr>`;
        }

        html += '</tbody></table>';
        document.getElementById('tabelaHistorico').innerHTML = html;

        // Paginação
        state.currentPage = dados.pagina;
        state.totalPaginas = dados.totalPaginas;
        renderizarPaginacao();
    } catch (e) {
        document.getElementById('tabelaHistorico').innerHTML = '<p style="color:var(--text-secondary)">Erro ao carregar histórico</p>';
    }
}

function renderizarPaginacao() {
    const container = document.getElementById('paginacao');
    let html = '';

    for (let i = 1; i <= state.totalPaginas; i++) {
        html += `<button class="${i === state.currentPage ? 'active' : ''}" onclick="carregarHistorico(${i})">${i}</button>`;
    }

    container.innerHTML = html;
}

async function deletarTeste(id) {
    if (!confirm('Deletar este teste?')) return;
    try {
        await fetch(`/api/historico/${id}`, { method: 'DELETE' });
        carregarHistorico(state.currentPage);
    } catch (e) {
        mostrarAlerta('Erro ao deletar');
    }
}

document.getElementById('btnFiltrar').addEventListener('click', () => carregarHistorico(1));

// ==================== CONFIG ====================
async function carregarAlertas() {
    try {
        const resp = await fetch('/api/teste/alertas');
        const dados = await resp.json();
        document.getElementById('alertaHabilitado').checked = dados.habilitado;
        document.getElementById('limiteCpu').value = dados.limiteCpu;
        document.getElementById('limiteCpuValor').textContent = dados.limiteCpu + '%';
        document.getElementById('limiteTemp').value = dados.limiteTemperatura;
        document.getElementById('limiteTempValor').textContent = dados.limiteTemperatura + '°C';
        document.getElementById('limiteRam').value = dados.limiteMemoria;
        document.getElementById('limiteRamValor').textContent = dados.limiteMemoria + '%';
    } catch (e) {}
}

// Sliders
['limiteCpu', 'limiteTemp', 'limiteRam'].forEach(id => {
    document.getElementById(id).addEventListener('input', (e) => {
        const suffix = id === 'limiteTemp' ? '°C' : '%';
        document.getElementById(id + 'Valor').textContent = e.target.value + suffix;
    });
});

document.getElementById('btnSalvarAlertas').addEventListener('click', async () => {
    const dados = {
        habilitado: document.getElementById('alertaHabilitado').checked,
        limiteCpu: parseFloat(document.getElementById('limiteCpu').value),
        limiteTemperatura: parseFloat(document.getElementById('limiteTemp').value),
        limiteMemoria: parseFloat(document.getElementById('limiteRam').value)
    };

    try {
        await fetch('/api/teste/alertas', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(dados)
        });
        mostrarAlerta('Configurações salvas!', 'sucesso');
    } catch (e) {
        mostrarAlerta('Erro ao salvar');
    }
});

// Exportação
document.getElementById('btnExportCsv').addEventListener('click', () => {
    window.location.href = '/api/relatorio/csv';
});

document.getElementById('btnExportPdf').addEventListener('click', () => {
    window.location.href = '/api/relatorio/pdf';
});

// ==================== ULTIMOS TESTES ====================
async function carregarUltimosTestes() {
    try {
        const resp = await fetch('/api/historico?tamanhoPagina=5');
        const dados = await resp.json();

        if (dados.testes.length === 0) {
            document.getElementById('ultimosTestes').innerHTML = '<p style="color:var(--text-secondary)">Nenhum teste executado ainda</p>';
            return;
        }

        let html = '<table><thead><tr>';
        html += '<th>Tipo</th><th>Nível</th><th>Duração</th><th>Métrica</th><th>Selo</th><th>Data</th>';
        html += '</tr></thead><tbody>';

        for (const t of dados.testes) {
            html += `<tr>
                <td>${t.tipo}</td>
                <td>${t.nivel || t.tipoDisco || ''}</td>
                <td>${t.duracaoSegundos}s</td>
                <td>${t.tipo === 'ESTRESSE' ? t.mediaCpu.toFixed(1) + '% CPU' : t.throughputMbps.toFixed(1) + ' MB/s'}</td>
                <td><span class="selo selo-${seloClasse(t.selo)}">${t.selo || '-'}</span></td>
                <td>${t.inicio}</td>
            </tr>`;
        }

        html += '</tbody></table>';
        document.getElementById('ultimosTestes').innerHTML = html;
    } catch (e) {}
}

// ==================== ALERTAS ====================
function mostrarAlerta(mensagem, tipo = 'erro') {
    const container = document.getElementById('alertasContainer');
    const toast = document.createElement('div');
    toast.className = 'alerta-toast';
    toast.style.background = tipo === 'sucesso' ? 'var(--success)' : 'var(--danger)';
    toast.textContent = mensagem;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 5000);
}

// ==================== INICIALIZACAO ====================
document.addEventListener('DOMContentLoaded', () => {
    inicializarGraficos();
    conectarWebSocket();
    carregarUltimosTestes();
});
