# GuidePC v3.0 - Monitoramento e Teste de Hardware via Web

Sistema completo de monitoramento e teste de hardware em Java 21, com dashboard web local, dados em tempo real via WebSocket e persistência em SQLite.

## Índice
1. [Visão Geral](#1-visão-geral)
2. [Funcionalidades](#2-funcionalidades)
3. [Arquitetura](#3-arquitetura)
4. [Tecnologias](#4-tecnologias)
5. [Requisitos](#5-requisitos)
6. [Como Rodar](#6-como-rodar)
7. [Endpoints da API](#7-endpoints-da-api)
8. [Estrutura do Projeto](#8-estrutura-do-projeto)
9. [Problemas Comuns](#9-problemas-comuns)

---

## 1. Visão Geral

O GuidePC lê hardware real do computador (CPU, RAM, disco, placa-mãe, BIOS, GPU) e oferece um dashboard web completo para monitoramento em tempo real, testes de estresse e disco, histórico de resultados e exportação de relatórios.

### O que mudou da v2.x para v3.0

| v2.x (Console) | v3.0 (Web) |
|---|---|
| Menu textual no terminal | Dashboard visual com gráficos em tempo real |
| Sem persistência | SQLite com histórico completo |
| Exportação local (relatorios/) | Exportação via botão no browser |
| Fechava com Ctrl+C | Servidor Javalin com abertura automática do browser |
| Jar ~6 MB | Jar ~15 MB (inclui Javalin + Jetty + Jackson) |

### O que NÃO faz

- Overclock ou ajustes de hardware
- Teste pesado de GPU
- Envio de dados para nuvem
- Multiusuário (é local apenas)

---

## 2. Funcionalidades

### Dashboard em Tempo Real
- CPU: uso%, temperatura, frequência, núcleos
- RAM: uso%, total, disponível
- GPU: uso%, temperatura (quando disponível)
- Gráficos Chart.js com histórico de 5 minutos

### Testes
- **Estresse CPU**: 3 níveis (Normal, Baixo, Alto) de 15s a 120s
- **Teste de Disco**: Leitura/Escrita Sequencial ou Aleatória
- Métricas ao vivo via WebSocket
- Selo de desempenho (Ótimo, Atenção, Crítico)

### Histórico e Relatórios
- SQLite com todos os testes realizados
- Filtros por tipo e nível
- Paginação
- Exportação CSV e PDF

### Configurações
- Alertas configuráveis (CPU, temperatura, RAM)
- Limites ajustáveis com sliders

---

## 3. Arquitetura

```
Browser (SPA)
    │
    ├─ WebSocket (/ws/monitoramento)  ← métricas a cada 1s
    │
    └─ HTTP REST (/api/*)
         │
         ├─ HardwareController    → coleta OSHI
         ├─ TesteController       → inicia/para testes
         ├─ HistoricoController   → consulta SQLite
         ├─ AlertaController      → configura limites
         └─ RelatorioController   → CSV/PDF
              │
              ├─ GerenciadorTestes (thread pool, callbacks WebSocket)
              ├─ ServicoColetorHardware (singleton, OSHI)
              ├─ ServicoAlerta (verificação de limites)
              └─ RepositorioResultados (SQLite CRUD)
```

**Fluxo de monitoramento:**
1. Browser abre `http://localhost:7000`
2. WebSocket conecta em `/ws/monitoramento`
3. Servidor envia métricas JSON a cada 1 segundo
4. Browser atualiza gráficos e cards

**Fluxo de teste:**
1. Usuário clica "Iniciar" no browser
2. POST para `/api/teste/estresse` ou `/api/teste/disco`
3. `GerenciadorTestes` executa em thread separada
4. Progresso e amostras enviados via WebSocket
5. Resultado salvo no SQLite e enviado ao browser

---

## 4. Tecnologias

| Camada | Ferramenta | Versão | Função |
|---|---|---|---|
| Backend | Java + Javalin | 21 + 7.1.0 | Servidor HTTP/WebSocket |
| Coleta | OSHI | 6.6.4 | Leitura de hardware |
| Persistência | SQLite JDBC | 3.53.2.1 | Banco de dados local |
| JSON | Jackson | 2.17.1 | Serialização |
| Frontend | HTML/CSS/JS | — | SPA com Chart.js |
| PDF | OpenPDF | 1.3.35 | Geração de relatórios |
| Build | Maven + Shade | 3.9+ / 3.6.0 | Empacotamento |

---

## 5. Requisitos

- JDK 21 (`java -version` para conferir)
- Maven 3.9+ (apenas para compilar)
- 200 MB livres
- Windows/Linux/macOS

---

## 6. Como Rodar

```bash
# Clonar
git clone https://github.com/Nitr0-Zeus/GuidePC.git
cd GuidePC

# Compilar
mvn clean package -DskipTests

# Rodar
java -jar target/guidepc-3.0.jar

# O browser abrirá automaticamente em http://localhost:7000
```

Para rodar os testes:
```bash
mvn test
```

---

## 7. Endpoints da API

### Hardware
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | `/api/hardware` | Dados completos do hardware |
| GET | `/api/hardware/cpu` | Dados da CPU |
| GET | `/api/hardware/memoria` | Dados da memória |
| GET | `/api/hardware/gpu` | Dados da GPU |

### Testes
| Método | URL | Descrição |
|--------|-----|-----------|
| POST | `/api/teste/estresse` | Iniciar teste de estresse |
| POST | `/api/teste/disco` | Iniciar teste de disco |
| POST | `/api/teste/parar` | Parar teste em execução |
| GET | `/api/teste/status` | Status do teste atual |

### Histórico
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | `/api/historico` | Listar testes (com filtros e paginação) |
| DELETE | `/api/historico/{id}` | Deletar teste |

### Configurações
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | `/api/teste/alertas` | Obter configuração de alertas |
| PUT | `/api/teste/alertas` | Atualizar configuração |

### Relatórios
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | `/api/relatorio/csv` | Exportar CSV |
| GET | `/api/relatorio/pdf` | Exportar PDF |

### WebSocket
| URL | Descrição |
|-----|-----------|
| `ws://localhost:7000/ws/monitoramento` | Métricas em tempo real (1s) |

---

## 8. Estrutura do Projeto

```
GuidePC/
├── pom.xml                                    # Dependências e configuração Maven
├── README.md                                  # Este arquivo
├── LICENSE
├── src/main/java/com/guidepc/
│   ├── GuidePCApplication.java               # Entry point
│   ├── modelo/                               # Records de dados
│   │   ├── InformacoesHardware.java
│   │   ├── InformacoesProcessador.java
│   │   ├── InformacoesMemoria.java
│   │   ├── InformacoesDisco.java
│   │   ├── InformacoesPlacaMae.java
│   │   ├── InformacoesSistemaOperacional.java
│   │   ├── NivelEstresse.java                # NORMAL / BAIXO / ALTO
│   │   ├── Amostra.java
│   │   ├── ResultadoTesteEstresse.java
│   │   ├── ResultadoTesteDisco.java
│   │   ├── ConfiguracaoAlerta.java
│   │   └── Alerta.java
│   ├── servico/                              # Lógica de negócio
│   │   ├── ServicoColetorHardware.java       # Coleta via OSHI (singleton)
│   │   ├── ServicoTesteEstresse.java         # Execução do teste de estresse
│   │   ├── ServicoTesteDisco.java            # Execução do teste de disco
│   │   ├── GerenciadorTestes.java            # Gerencia testes async + WebSocket
│   │   ├── ServicoMonitoramento.java         # Configuração de alertas
│   │   ├── ServicoAlerta.java                # Verificação de limites
│   │   ├── ServicoComparacao.java            # Comparação de resultados
│   │   ├── ExportadorCsv.java                # Geração de CSV
│   │   └── ExportadorPdf.java                # Geração de PDF
│   ├── persistencia/                         # Acesso ao banco
│   │   ├── ConexaoBanco.java                 # Conexão SQLite (singleton)
│   │   ├── MigracaoBanco.java                # Criação das tabelas
│   │   └── RepositorioResultados.java        # CRUD de testes
│   ├── web/                                  # Servidor HTTP
│   │   ├── ServidorWeb.java                  # Configuração Javalin
│   │   ├── controlador/                      # Controllers REST
│   │   │   ├── HardwareController.java
│   │   │   ├── TesteController.java
│   │   │   ├── HistoricoController.java
│   │   │   ├── AlertaController.java
│   │   │   └── RelatorioController.java
│   │   └── websocket/
│   │       └── MonitoramentoWs.java          # WebSocket em tempo real
│   └── utilitario/
│       ├── Formatador.java                   # Formatação de valores
│       └── NomeArquivo.java                  # Geração de nomes de arquivo
├── src/main/resources/public/                # Frontend estático
│   ├── index.html                            # SPA shell
│   ├── css/style.css                         # Estilos (dark theme)
│   └── js/app.js                             # JavaScript do frontend
└── src/test/java/                            # Testes JUnit
```

---

## 9. Problemas Comuns

| Problema | Causa | Solução |
|----------|-------|---------|
| `Temperatura: N/A` | Sem sensor ou sem permissão | Normal em alguns PCs; não afeta o teste |
| `MSAcpi_ThermalZoneTemperature` warn | OSHI não encontra WMI | Ignorar (log do SLF4J) |
| Porta 7000 em uso | Outro programa na porta | Feche o programa ou altere a porta em `GuidePCApplication.java` |
| `database is locked` | Concorrent SQLite | Resolver com `PRAGMA busy_timeout` (já configurado) |
| Browser não abre | Pode estar bloqueado | Acesse manualmente `http://localhost:7000` |

---

## Licença

MIT
