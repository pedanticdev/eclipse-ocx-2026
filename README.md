# Eclipse OCX 2026 - Jakarta Data + Virtual Threads AI Assistant

A RAG-based AI microservice built with Jakarta EE 11, Jakarta Data repositories, virtual threads, and Java cosine similarity. No Python. No vector database. No thread pool tuning.

Talk: **Jakarta EE 11 Meets AI: Building Intelligent Microservices with Virtual Threads and Jakarta Data** - Eclipse OCX 2026, Brussels.

## Quick Start

```bash
./run.sh deploy
```

Opens at [http://localhost:8080](http://localhost:8080). First run pulls AI models (~2 GB).

## Prerequisites

- Docker
- 4 GB RAM minimum (8 GB recommended)

No local JDK or Maven needed. The Dockerfile builds the WAR inside the container.

## Commands

```
./run.sh deploy     Full setup: infra + build + deploy
./run.sh start      Start existing services
./run.sh stop       Stop all services
./run.sh restart    Stop and redeploy
./run.sh truncate   Clear embeddings and re-ingest
./run.sh logs       Tail application logs
./run.sh status     Show service status
./run.sh clean      Remove everything including data
```

## Architecture

```
Browser (HTML + HTMX)
  -> Payara Micro (JAX-RS + CDI)
    -> AiService (programmatic RAG)
      -> Ollama (local LLM inference)
    -> PostgreSQL (Jakarta Data repositories)
    -> VectorSearch (Java cosine similarity)
    -> Virtual Threads (concurrent embedding + benchmark)
```

Six Jakarta EE specifications and one MicroProfile spec:

| Specification | Role |
|---|---|
| Jakarta Data 1.0 | `@Repository` interfaces with `@Find` and `@Query` |
| Jakarta Concurrency 3.1 | `@ManagedExecutorDefinition(virtual=true)` |
| CDI 4.1 | Bean producers, model factory, application-scoped services |
| JPA 3.2 | Entities with `byte[]` embeddings, `@DataSourceDefinition` |
| JAX-RS | Chat, model switching, benchmark endpoints |
| MicroProfile Config | `@ConfigProperty` for model names, Ollama URL, timeouts |

## What's Inside

**`DocumentChunk`** - JPA entity storing text segments and embeddings as `byte[]`. Conversion to `float[]` via `EmbeddingConverter`.

**`ConferenceTalk`** - JPA entity for structured conference data (title, abstract, speaker, track, time slot).

**`Chunks`, `Talks`** - Jakarta Data repositories. Zero implementation classes. `BasicRepository` gives CRUD, `@Find` and `@Query` add custom queries.

**`VectorSearch`** - 15 lines of cosine similarity in Java. Loads chunks from the repository, computes similarity against the query embedding, returns top matches. No vector database required.

**`AiService`** - Programmatic RAG pipeline. Embed query, find similar chunks, build prompt, call LLM. ~25 lines. Zero framework magic.

**`ChatModelFactory`** - Runtime model switching via `volatile` fields. Switch from gemma4 to mistral with one HTTP POST. No redeployment.

**`EmbeddingProducer`** - CDI producer for the `EmbeddingModel` bean.

**`BenchmarkService`** - Fires N concurrent AI requests through managed virtual threads. Measures wall-clock time, latency, and throughput.

**`DataLoader`** - Seeds conference talks, splits into chunks, generates embeddings in parallel using virtual threads at startup.

**`ConcurrencyConfig`** - `@ManagedExecutorDefinition(virtual=true)` with a CDI qualifier. Container-managed lifecycle, automatic context propagation.

**`ChatResource`** - JAX-RS POST endpoint. Receives HTMX form posts, returns HTML fragments.

**`ModelResource`** - GET returns current model and available models as JSON. POST switches the model at runtime.

**`BenchmarkResource`** - Preset question lists (5, 10, 20). Returns HTML benchmark results with timing metrics.

## AI Models

| Model | Purpose | Size |
|---|---|---|
| gemma4:e2b | Chat (answer generation) | ~7.2 GB |
| mistral | Chat (alternative model) | ~4.1 GB |
| nomic-embed-text | Embeddings (vector search) | ~274 MB |

## Runtime Model Switching

Send one HTTP POST to swap models without redeployment:

```bash
curl -X POST http://localhost:8080/api/models -d "model=mistral"
```

The next chat request uses the new model. The `ChatModelFactory` holds the `ChatModel` in a `volatile` field for thread-safe switching.

## Switching to GPU

Change one environment variable:

```yaml
environment:
  - OLLAMA_BASE_URL=http://gpu-server:11434
```

Same WAR, zero code changes.

## Project Structure

```
src/main/java/com/azul/eclipseocx2026/
  ApplicationConfig.java           JAX-RS application path
  config/
    ConcurrencyConfig.java         @ManagedExecutorDefinition(virtual=true)
    DataSourceConfig.java          @DataSourceDefinition for PostgreSQL
    VirtualThreadExecutor.java     CDI qualifier for managed executor
  ai/
    AiService.java                 Programmatic RAG pipeline
    BenchmarkService.java          Concurrent AI request benchmarking
    ChatModelFactory.java          Runtime model switching
    EmbeddingProducer.java         CDI producer for EmbeddingModel
    VectorSearch.java              Java cosine similarity
  data/
    DataLoader.java                Startup seeding + virtual thread embedding
  model/
    ConferenceTalk.java            Conference talk JPA entity
    DocumentChunk.java             Text chunk + embedding JPA entity
    EmbeddingConverter.java        byte[] <-> float[] conversion
  repository/
    Chunks.java                    @Repository for DocumentChunk
    Talks.java                     @Repository for ConferenceTalk
  resource/
    BenchmarkResource.java         POST /api/benchmark
    ChatResource.java              POST /api/chat
    ModelResource.java             GET/POST /api/models

src/main/webapp/
  index.html                       Chat UI + model switcher + benchmark
  presentation.html                Reveal.js slide deck
  css/style.css                    Light theme
  js/htmx.min.js                   HTMX library
```

## What We Don't Use

| Not used | Instead |
|---|---|
| PgVector / Pinecone / Weaviate | `byte[]` in JPA entity + Java cosine similarity |
| `@RegisterAIService` (declarative) | Programmatic `AiService` with `ChatModelFactory` |
| `langchain4j-cdi-ext` | Standard CDI producers |
| `EmbeddingStore` | `VectorSearch` class (15 lines) |
| Raw `Executors.newVirtualThreadPerTaskExecutor()` | `@ManagedExecutorDefinition(virtual=true)` |
| `ConfigProvider.getConfig()` | `@Inject @ConfigProperty` |

More code, but zero magic. Every step is visible, debuggable, and replaceable.

## License

MIT
