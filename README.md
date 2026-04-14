# Eclipse OCX 2026 - Jakarta EE AI Assistant

A RAG-based AI assistant built entirely within a Jakarta EE 11 WAR. No Python. No API keys. No microservices. Local LLM inference via Ollama, vector storage via PgVector, all wired together with CDI.

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
./run.sh truncate   Clear embeddings and re-ingest
./run.sh benchmark  Run benchmark (cpu or gpu)
./run.sh logs       Tail application logs
./run.sh status     Show service status
./run.sh clean      Remove everything including data
```

## Architecture

```
Browser (HTML + HTMX)
  → Payara Micro (JAX-RS + CDI)
    → LangChain4j (RAG pipeline)
      → Ollama (local LLM inference)
    → PostgreSQL (pgvector extension)
```

Five Jakarta EE specifications drive the implementation:

| Specification | Role |
|---|---|
| CDI 4.1 | Bean producers for AI models, event-driven ingestion |
| JPA 3.2 | Document entities alongside vector embeddings |
| JAX-RS | Chat and benchmark endpoints, server-rendered HTML |
| MicroProfile Config | Externalize `OLLAMA_BASE_URL` |
| Jakarta Persistence | Schema generation, `@DataSourceDefinition` |

## What's Inside

**`EmbeddingProducers`** - Three CDI producer methods that create the `EmbeddingModel`, `EmbeddingStore`, and `ContentRetriever` as injectable beans.

**`DocumentEntity`** - JPA entity persisted in PostgreSQL. The same database holds both relational data and vector embeddings.

**`DataLoader`** - On startup, persists Jakarta EE spec documents as JPA entities, then splits and embeds them into PgVector. Duplicate guard prevents re-ingestion on restart.

**`ConferenceAssistant`** - A LangChain4j interface with `@SystemMessage` and `@UserMessage` annotations. No implementation class; LangChain4j generates it at runtime.

**`ConferenceChatService`** - Wires the AI service via `AiServices.builder()`. Detects greetings to bypass RAG for non-query inputs.

**`ChatResource`** - JAX-RS POST endpoint. Receives HTMX form posts, returns HTML fragments. Server-side markdown rendering via CommonMark.

**`BenchmarkResource`** - Live benchmark runner and static CPU vs GPU comparison table.

## AI Models

| Model | Purpose | Size |
|---|---|---|
| gemma4:e2b | Chat (answer generation) | ~1.6 GB |
| nomic-embed-text | Embeddings (vector search) | ~274 MB |

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
  ApplicationConfig.java       JAX-RS application path
  config/
    DataSourceConfig.java      @DataSourceDefinition for PostgreSQL
  ai/
    EmbeddingProducers.java    CDI producers for model, store, retriever
    ConferenceAssistant.java   AI service interface
    ConferenceChatService.java RAG wiring + greeting detection
  data/
    DocumentEntity.java        JPA entity
    DataLoader.java            Persist + embed on startup
  resource/
    ChatResource.java          POST /api/chat
    BenchmarkResource.java     GET /api/benchmark, GET /api/benchmark/run

src/main/webapp/
  index.html                   Chat UI (HTMX)
  presentation.html            Reveal.js slide deck
  css/style.css                Dark theme
  js/htmx.min.js               HTMX library
```

## License

MIT
