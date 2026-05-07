# Eclipse OCX 2026

Jakarta EE 11 conference materials for the Eclipse OCX 2026 conference, Brussels. The `main` branch is the seed: a Jakarta EE 11 WAR skeleton with a Hello-World JAX-RS resource. Each feature branch below grafts a different conference narrative onto it.

## Branches

| Branch | Format | What It Shows |
|---|---|---|
| `talk/jakarta-data-ai` | Conference talk | Jakarta Data + Virtual Threads RAG. `byte[]` embeddings stored on JPA entities, Java cosine similarity, runtime model switching with LangChain4j and Ollama. |
| `blog/pure-jakarta` | Blog post | Same RAG architecture, no LangChain4j. `OllamaChat` and `OllamaEmbeddings` use only `java.net.http.HttpClient` and `jakarta.json`. |
| `talk/intelligent-monolith` | Conference talk | Four AI runtimes in one WAR: declarative LangChain4j, LangGraph4j stateful agents, Koog tool-calling, Jlama in-JVM inference. MicroProfile Fault Tolerance and a custom `@Audited` interceptor wrap every call. |

Each branch ships its own `README.md` with build, deploy, and demo instructions.

## Skeleton (this branch)

Main has the minimum Jakarta EE 11 setup:

- Maven WAR packaging, `finalName=ocx`
- `jakarta.jakartaee-web-api 11.0.0` (provided scope)
- A `@Path("/hello-world")` JAX-RS resource as a smoke test
- `beans.xml` and `persistence.xml` ready for CDI and JPA

```bash
./mvnw package
# target/ocx.war ready for Payara Micro 7
```

To run any of the conference branches, check it out and follow that branch's README:

```bash
git checkout talk/jakarta-data-ai
./run.sh deploy
```

## Project Layout (main)

```
src/main/
  java/com/azul/eclipseocx2026/
    HelloApplication.java   @ApplicationPath("/api")
    HelloResource.java      @Path("/hello-world") JAX-RS resource
  resources/
    META-INF/
      beans.xml             CDI bean discovery
      persistence.xml       JPA persistence unit
```

## License

MIT
