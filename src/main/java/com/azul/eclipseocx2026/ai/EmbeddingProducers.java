package com.azul.eclipseocx2026.ai;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import javax.sql.DataSource;
import java.time.Duration;

@ApplicationScoped
public class EmbeddingProducers {

    private static final String OLLAMA_BASE_URL =
            System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");

    @Resource(lookup = "java:app/ocx-db")
    DataSource dataSource;

    @Produces
    @ApplicationScoped
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(OLLAMA_BASE_URL)
                .modelName("nomic-embed-text")
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Produces
    @ApplicationScoped
    public EmbeddingStore<TextSegment> embeddingStore() {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .dimension(768)
                .table("embeddings")
                .createTable(true)
                .build();
    }

    @Produces
    @ApplicationScoped
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> store, EmbeddingModel model) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(model)
                .maxResults(5)
                .minScore(0.5)
                .build();
    }
}
