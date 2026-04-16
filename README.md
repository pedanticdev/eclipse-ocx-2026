# Eclipse OCX 2026 - The Intelligent Monolith

Four progressive AI modes in a single Jakarta EE 11 WAR. No Python. No API keys. No microservices. From declarative RAG to in-process LLM inference, all wired together with CDI.

Talk: **The Intelligent Monolith: Supercharging Jakarta EE with Local AI** - Eclipse OCX 2026, Brussels.

## Quick Start

```bash
./run.sh deploy
```

Opens at [http://localhost:8080](http://localhost:8080). First run pulls AI models (~2 GB), subsequent starts are faster.

## Prerequisites

- Docker
- 4 GB RAM minimum (8 GB recommended)

That's it. The Dockerfile builds the WAR inside the container. No local JDK or Maven needed.

## Commands

```
./run.sh deploy     Full setup: infra + build + deploy
./run.sh start      Start existing services
./run.sh stop       Stop all services
./run.sh restart    Stop and redeploy
./run.sh jlama      Deploy with in-process Jlama (no Ollama needed)
./run.sh truncate   Clear embeddings and re-ingest
./run.sh benchmark  Run benchmark (cpu or gpu)
./run.sh logs       Tail application logs
./run.sh status     Show service status
./run.sh clean      Remove everything including data
```

## Architecture

```
Browser (HTML + HTMX)
  → Payara Micro 7 (JAX-RS + CDI)
    → Mode 1: LangChain4j AiServices (declarative RAG)
    → Mode 2: LangGraph4j (stateful agentic workflow)
    → Mode 3: Koog (agent composition with specialist tools)
    → Mode 4: Jlama (in-process LLM inference, no external model server)
      ↕ PostgreSQL + pgvector (embeddings + relational data)
      ↕ Ollama (local LLM inference for modes 1-3)
```

Each mode builds on the previous, demonstrating progressive AI adoption within a monolithic Jakarta EE application.

## The Four Modes

| Mode | Framework | What It Does |
|---|---|---|
| Declarative RAG | LangChain4j AiServices | Question → embedding search → context-augmented LLM response |
| Agentic Workflow | LangGraph4j | LLM classifies query → routes to RAG or direct answer → responds |
| Agent Composition | Koog | Planner agent with 3 specialist tools (spec search, version check, code generation) |
| In-Process Inference | Jlama | LLM inference inside the JVM. No Ollama needed. |

## Jakarta EE & MicroProfile Specifications

| Specification | Role |
|---|---|
| CDI 4.1 | Bean producers, interceptors, event-driven ingestion |
| JPA 3.2 | Document entities alongside vector embeddings |
| JAX-RS | Chat and benchmark endpoints, server-rendered HTML |
| Jakarta Interceptors | Audit logging via `@Audited` binding |
| MicroProfile Config | Externalize model names, URLs, thresholds |
| MicroProfile Fault Tolerance | `@CircuitBreaker`, `@Retry`, `@Fallback` on AI calls |

## What's Inside

### AI Pipeline

**`EmbeddingProducers`** - Three CDI producer methods that create the `EmbeddingModel`, `EmbeddingStore`, and `ContentRetriever` as injectable beans.

**`DocumentEntity`** - JPA entity persisted in PostgreSQL. The same database holds both relational data and vector embeddings.

**`EmbeddingPreComputer`** - Build-time standalone tool (run via Maven profile). Parses Jakarta EE spec HTML files with Jsoup, splits into 1000-char chunks with 200-char overlap, embeds via Ollama `nomic-embed-text`, and writes JSON files with text + float[] vectors.

**`PrecomputedEmbeddingLoader`** - Runtime reader using Jakarta JSON-P. Parses the pre-computed embedding JSON files into `TextSegment` + `Embedding` pairs for direct insertion into PgVector.

**`DataLoader`** - On startup, loads Jakarta EE spec embeddings into PgVector. Two-tier strategy: first checks for pre-computed JSON files (generated at build time by `EmbeddingPreComputer`), then falls back to parsing the HTML specification documents and embedding them live via Ollama. Batches embeddings in groups of 50 to avoid timeouts. Duplicate guard prevents re-ingestion on restart.

**`ConferenceAssistant`** - A LangChain4j interface with `@SystemMessage` and `@UserMessage` annotations. No implementation class; LangChain4j generates it at runtime.

### Agentic Modes

**`ConferenceAgent`** - LangGraph4j state graph: `START → classify → (rag | direct) → respond → END`. The LLM classifies each query to decide whether RAG retrieval is needed.

**`ConferenceOrchestrator`** - Koog `AIAgent` with three specialist tools registered via `ToolRegistryBuilder`. The planner agent decides which tools to call based on the question.

**`JlamaChatModelProducer`** - CDI bean that loads a Jlama model for in-JVM inference. Initializes gracefully: if the model is unavailable, the other three modes continue working.

### Specialist Tools (Koog)

| Tool | Access Scope |
|---|---|
| `SpecSearchTool` | Reads PgVector via `ContentRetriever`. Bounded by `maxResults(5)` and `minScore(0.5)`. |
| `VersionCheckTool` | Static in-memory map of Jakarta EE version history. No external access. |
| `CodeExampleTool` | Generates code examples via the LLM. Text output only. |

### Security

**`ChatResource`** - Input validation before any LLM prompt: length cap (500 chars), mode allowlist, and regex-based injection pattern detection.

**`AuditInterceptor`** - CDI interceptor (`@Audited` binding + `@Priority`) that produces structured log entries for every agent interaction: method, parameters, result length, latency, status.

**`ConferenceChatService`** - MicroProfile Fault Tolerance annotations: `@Retry` (2 retries, 1s backoff), `@CircuitBreaker` (trips after 3/5 failures, 30s half-open), `@Fallback` (returns service-unavailable message).

### HTTP Layer

**`ChatResource`** - JAX-RS POST endpoint. Receives HTMX form posts, returns HTML fragments. Server-side markdown rendering via CommonMark.

**`BenchmarkResource`** - Live benchmark runner and static CPU vs GPU comparison table.

## AI Models

| Model | Purpose | Size | Mode |
|---|---|---|---|
| gemma4:e2b | Chat (answer generation) | ~1.6 GB | 1-3 via Ollama |
| nomic-embed-text | Embeddings (vector search) | ~274 MB | 1-3 via Ollama |
| gemma-2b-it-jlama-Q4 | In-process inference | ~1.4 GB | 4 via Jlama |

## Data Ingestion Pipeline

The RAG pipeline ingests full Jakarta EE specification documents (not summaries). Five specs totaling ~11 MB of HTML content, split into ~3500 segments of 1000 characters with 200-character overlap.

### Two-Tier Loading Strategy

1. **Pre-computed (fast startup):** Run `mvn process-resources -Pprecompute-embeddings` to generate JSON embedding files at build time. The `DataLoader` loads these in milliseconds at startup.
2. **Runtime fallback (no build step):** If pre-computed files are absent, `DataLoader` parses the HTML specs with Jsoup, splits them, and calls Ollama for embedding in batches of 50 segments. This takes ~13 minutes on first startup.

### Downloading Spec Documents

```bash
./download-specs.sh
```

Downloads the 5 Jakarta EE specification HTML files from [jakarta.ee/specifications](https://jakarta.ee/specifications/) into `src/main/resources/data/jakarta-specs/`.

### Pre-computing Embeddings

```bash
# Requires Ollama running locally with nomic-embed-text pulled
mvn process-resources -Pprecompute-embeddings
```

Generates JSON files in `target/classes/data/embeddings/` containing pre-computed vectors for each spec. Package these into the WAR for instant startup.

## Switching to GPU

Change one environment variable:

```yaml
environment:
  - OLLAMA_BASE_URL=http://gpu-server:11434
```

Ollama auto-detects GPU hardware (CUDA for NVIDIA, ROCm for AMD). Same WAR, zero code changes.

## Project Structure

```
src/main/java/com/azul/eclipseocx2026/
  ApplicationConfig.java         JAX-RS application path
  build/
    EmbeddingPreComputer.java    Build-time tool: HTML specs -> pre-computed embedding JSON
  config/
    DataSourceConfig.java        @DataSourceDefinition for PostgreSQL
  ai/
    EmbeddingProducers.java      CDI producers for model, store, retriever
    ConferenceAssistant.java     AI service interface (LangChain4j)
    ConferenceChatService.java   Mode routing + Fault Tolerance + @Audited
    ConferenceAgent.java         LangGraph4j agentic workflow
    ConferenceOrchestrator.java  Koog agent composition
    JlamaChatModelProducer.java  In-process inference (graceful opt-in)
    tools/
      SpecSearchTool.java        RAG search via ContentRetriever
      VersionCheckTool.java      Jakarta EE version lookup
      CodeExampleTool.java       LLM-powered code generation
  security/
    Audited.java                 CDI interceptor binding
    AuditInterceptor.java        Structured audit logging
  data/
    DocumentEntity.java          JPA entity
    DataLoader.java              Precomputed-first loading + HTML fallback + batch embedding
    PrecomputedEmbeddingLoader.java  Runtime reader for build-time embedding JSON
  resource/
    ChatResource.java            POST /api/chat (input validation)
    BenchmarkResource.java       GET /api/benchmark, GET /api/benchmark/run

src/main/webapp/
  index.html                     Chat UI (4 tabs, HTMX)
  presentation.html              Reveal.js slide deck
  css/style.css                  Dark theme
  js/htmx.min.js                 HTMX library

src/main/resources/
  META-INF/microprofile-config.properties
  data/
    jakarta-specs/               Full Jakarta EE specification HTML documents
      cdi.html                   CDI 4.1 spec (~722 KB)
      jpa.html                   JPA 3.2 spec (~2.5 MB)
      concurrency.html           Concurrency 3.1 spec (~6.9 MB)
      data.html                  Data 1.0 spec (~541 KB)
      jaxrs.html                 JAX-RS 4.0 spec (~553 KB)
    embeddings/                  (generated) Pre-computed embedding JSON files

download-specs.sh                Downloads Jakarta EE HTML specs from jakarta.ee
```

## License

MIT
