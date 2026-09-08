# GuidePC v4.0 — Monitoramento e Teste de Hardware via Web

Sistema completo de monitoramento e teste de hardware em Java 21, com dashboard web local, dados em tempo real via WebSocket e persistencia em SQLite.

## Indice
1. [Visao Geral](#1-visao-geral)
2. [Funcionalidades](#2-funcionalidades)
3. [Arquitetura](#3-arquitetura)
4. [Tecnologias](#4-tecnologias)
5. [Requisitos](#5-requisitos)
6. [Como Rodar](#6-como-rodar)
7. [Endpoints da API](#7-endpoints-da-api)
8. [Estrutura do Projeto](#8-estrutura-do-projeto)
9. [Problemas Comuns](#9-problemas-comuns)

---

## 1. Visao Geral

O GuidePC le hardware real do computador (CPU, RAM, disco, placa-mae, BIOS, GPU, bateria) e oferece um dashboard web completo para monitoramento em tempo real, testes de estresse e disco, historico de resultados e exportacao de relatorios.

### O que mudou da v3.0 para v4.0

| v3.0 | v4.0 |
|---|---|
| Tema escuro fixo | Tema claro/escuro com toggle |
| Sem pagina de hardware | Pagina dedicada com detalhes do sistema |
| Sem monitoramento de bateria | Card de bateria em notebooks |
| Sem espaco em disco | Barra de espaco por particao |
| Sem graficos de temperatura/GPU | Graficos dedicados de temperatura e GPU |
| Sem detalhe de amostras | Modal com grafico de amostras por teste |
| Porta 7000 fixa | Porta configuravel (default 7070) |
| Sem shutdown hook | Shutdown hook para limpeza de recursos |

### O que NAO faz

- Overclock ou ajustes de hardware
- Teste pesado de GPU
- Envio de dados para nuvem
- Multiusuario (e local apenas)

---

## 2. Funcionalidades

### Dashboard em Tempo Real
- CPU: uso%, temperatura, frequencia, nucleos
- RAM: uso%, total, disponivel
- GPU: uso%, temperatura (quando disponivel)
- Bateria: percentual, tempo restante, estado de carga (notebooks)
- Espaco em disco: barras de uso por particao
- Graficos Chart.js com historico de 5 minutos (CPU, RAM, Temperatura, GPU)

### Pagina de Hardware
- Informacoes detalhadas de CPU, RAM, Disco, Placa Mae, SO e GPU
- Dados completos: fabricante, modelo, microarquitetura, nucleos, frequencias

### Testes
- **Estresse CPU**: 3 niveis (Normal, Baixo, Alto) de 5s a 600s
- **Teste de Disco**: Leitura/Escrita Sequencial ou Aleatoria de 5s a 120s
- Metricas ao vivo via WebSocket
- Selo de desempenho (Otimo, Atencao, Critico)
- Modal de detalhes com grafico de amostras por teste

### Historico e Relatorios
- SQLite com todos os testes realizados
- Filtros por tipo e nivel
- Paginacao
- Exportacao CSV e PDF

### Tema
- Toggle entre tema escuro (padrao) e tema claro
- Preferencia salva no localStorage

### Configuracoes
- Alertas configuraveis (CPU, temperatura, RAM)
- Limites ajustaveis com sliders

---

## 3. Arquitetura

```
Browser (SPA)
    |
    +-- WebSocket (/ws/monitoramento)  <- metricas a cada 1s
    |
    +-- HTTP REST (/api/*)
         |
         +-- HardwareController    -> coleta OSHI + disco espaco
         +-- TesteController       -> inicia/para testes
         +-- HistoricoController   -> consulta SQLite + amostras
         +-- AlertaController      -> configura limites
         +-- RelatorioController   -> CSV/PDF
              |
              +-- GerenciadorTestes (thread pool, callbacks WebSocket)
              +-- ServicoColetorHardware (singleton, OSHI)
              +-- ServicoAlerta (verificacao de limites)
              +-- RepositorioResultados (SQLite CRUD)
```

**Fluxo de monitoramento:**
1. Browser abre `http://localhost:7070`
2. WebSocket conecta em `/ws/monitoramento`
3. Servidor envia metricas JSON a cada 1 segundo (incluindo bateria)
4. Browser atualiza graficos e cards

**Fluxo de teste:**
1. Usuario clica "Iniciar" no browser
2. POST para `/api/teste/estresse` ou `/api/teste/disco`
3. `GerenciadorTestes` executa em thread separada
4. Progresso e amostras enviados via WebSocket
5. Resultado salvo no SQLite e enviado ao browser

---

## 4. Tecnologias

| Camada | Ferramenta | Versao | Funcao |
|---|---|---|---|
| Backend | Java + Javalin | 21 + 7.1.0 | Servidor HTTP/WebSocket |
| Coleta | OSHI | 6.6.4 | Leitura de hardware |
| Persistencia | SQLite JDBC | 3.53.2.1 | Banco de dados local |
| JSON | Jackson | 2.17.1 | Serializacao |
| Frontend | HTML/CSS/JS | — | SPA com Chart.js |
| PDF | OpenPDF | 1.3.35 | Geracao de relatorios |
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
java -jar target/guidepc-4.0.jar

# O browser abrira automaticamente em http://localhost:7070
```

Para rodar os testes:
```bash
mvn test
```

---

## 7. Endpoints da API

### Hardware
| Metodo | URL | Descricao |
|--------|-----|-----------|
| GET | `/api/hardware` | Dados completos do hardware |
| GET | `/api/hardware/cpu` | Dados da CPU |
| GET | `/api/hardware/memoria` | Dados da memoria |
| GET | `/api/hardware/gpu` | Dados da GPU |
| GET | `/api/hardware/disco-espaco` | Espaco usado por particao |

### Testes
| Metodo | URL | Descricao |
|--------|-----|-----------|
| POST | `/api/teste/estresse` | Iniciar teste de estresse |
| POST | `/api/teste/disco` | Iniciar teste de disco |
| POST | `/api/teste/parar` | Parar teste em execucao |
| GET | `/api/teste/status` | Status do teste atual |

### Historico
| Metodo | URL | Descricao |
|--------|-----|-----------|
| GET | `/api/historico` | Listar testes (com filtros e paginacao) |
| GET | `/api/historico/{id}/amostras` | Amostras de um teste especifico |
| DELETE | `/api/historico/{id}` | Deletar teste |

### Configuracoes
| Metodo | URL | Descricao |
|--------|-----|-----------|
| GET | `/api/teste/alertas` | Obter configuracao de alertas |
| PUT | `/api/teste/alertas` | Atualizar configuracao |

### Relatorios
| Metodo | URL | Descricao |
|--------|-----|-----------|
| GET | `/api/relatorio/csv` | Exportar CSV |
| GET | `/api/relatorio/pdf` | Exportar PDF |

### WebSocket
| URL | Descricao |
|-----|-----------|
| `ws://localhost:7070/ws/monitoramento` | Metricas em tempo real (1s) |

---

## 8. Estrutura do Projeto

```
GuidePC/
├── pom.xml                                    # Dependencias e configuracao Maven
├── README.md                                  # Este arquivo
├── LICENSE
├── src/main/java/com/guidepc/
│   ├── GuidePCApplication.java               # Entry point + shutdown hook
│   ├── modelo/                               # Records de dados
│   │   ├── InformacoesHardware.java
│   │   ├── InformacoesProcessador.java
│   │   ├── InformacoesMemoria.java
│   │   ├── InformacoesDisco.java
│   │   ├── InformacoesPlacaMae.java
│   │   ├── InformacoesSistemaOperacional.java
│   │   ├── InformacoesBateria.java           # Dados de bateria (notebooks)
│   │   ├── InformacoesDiscoParticao.java     # Espaco por particao
│   │   ├── NivelEstresse.java                # NORMAL / BAIXO / ALTO
│   │   ├── Amostra.java
│   │   ├── ResultadoTesteEstresse.java
│   │   ├── ResultadoTesteDisco.java
│   │   ├── ConfiguracaoAlerta.java
│   │   └── Alerta.java
│   ├── servico/                              # Logica de negocio
│   │   ├── ServicoColetorHardware.java       # Coleta via OSHI (singleton)
│   │   ├── ServicoTesteEstresse.java         # Execucao do teste de estresse
│   │   ├── ServicoTesteDisco.java            # Execucao do teste de disco
│   │   ├── GerenciadorTestes.java            # Gerencia testes async + WebSocket
│   │   ├── ServicoMonitoramento.java         # Configuracao de alertas
│   │   ├── ServicoAlerta.java                # Verificacao de limites
│   │   ├── ServicoComparacao.java            # Comparacao de resultados
│   │   ├── ExportadorCsv.java                # Geracao de CSV
│   │   └── ExportadorPdf.java                # Geracao de PDF
│   ├── persistencia/                         # Acesso ao banco
│   │   ├── ConexaoBanco.java                 # Conexao SQLite (singleton)
│   │   ├── MigracaoBanco.java                # Criacao das tabelas
│   │   └── RepositorioResultados.java        # CRUD de testes
│   ├── web/                                  # Servidor HTTP
│   │   ├── ServidorWeb.java                  # Configuracao Javalin
│   │   ├── controlador/                      # Controllers REST
│   │   │   ├── HardwareController.java
│   │   │   ├── TesteController.java
│   │   │   ├── HistoricoController.java
│   │   │   ├── AlertaController.java
│   │   │   └── RelatorioController.java
│   │   └── websocket/
│   │       └── MonitoramentoWs.java          # WebSocket em tempo real
│   └── utilitario/
│       ├── Formatador.java                   # Formatacao de valores
│       ├── VersaoApp.java                    # Versao centralizada
│       └── NomeArquivo.java                  # Geracao de nomes de arquivo
├── src/main/resources/public/                # Frontend estatico
│   ├── index.html                            # SPA shell
│   ├── css/style.css                         # Estilos (dark/light theme)
│   └── js/
│       ├── app.js                            # JavaScript do frontend
│       └── tema.js                           # Controle de tema claro/escuro
└── src/test/java/                            # Testes JUnit
```

---

## 9. Problemas Comuns

| Problema | Causa | Solucao |
|----------|-------|---------|
| `Temperatura: N/A` | Sem sensor ou sem permissao | Normal em alguns PCs; nao afeta o teste |
| `MSAcpi_ThermalZoneTemperature` warn | OSHI nao encontra WMI | Ignorar (log do SLF4J) |
| Porta 7070 em uso | Outro programa na porta | Feche o programa ou altere a porta em `GuidePCApplication.java` |
| `database is locked` | Concorrent SQLite | Resolver com `PRAGMA busy_timeout` (ja configurado) |
| Browser nao abre | Pode estar bloqueado | Acesse manualmente `http://localhost:7070` |
| Bateria nao aparece | Desktop sem bateria | Normal; card so aparece em notebooks |

---

## Licenca

MIT
