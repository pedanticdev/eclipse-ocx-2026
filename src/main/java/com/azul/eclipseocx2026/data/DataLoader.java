package com.azul.eclipseocx2026.data;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@ApplicationScoped
public class DataLoader {

    private static final Logger LOG = Logger.getLogger(DataLoader.class.getName());

    @Inject
    private EmbeddingModel embeddingModel;

    @Inject
    private EmbeddingStore<TextSegment> embeddingStore;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object event) {
        LOG.info("Starting data ingestion...");

        List<Document> documents = loadJakartaDocs();
        ingest(documents);

        LOG.info("Data ingestion complete. " + documents.size() + " documents loaded.");
    }

    private List<Document> loadJakartaDocs() {
        List<Document> documents = new ArrayList<>();
        String[] docFiles = {"cdi.txt", "jpa.txt", "concurrency.txt", "data.txt", "jaxrs.txt"};

        DocumentParser parser = new TextDocumentParser();
        for (String filename : docFiles) {
            InputStream is = getClass().getResourceAsStream("/data/jakarta-docs/" + filename);
            if (is != null) {
                Document doc = parser.parse(is);
                documents.add(Document.from(
                        doc.text(),
                        new Metadata(Map.of("source", "jakarta-docs", "title", filename.replace(".txt", "")))
                ));
            }
        }
        return documents;
    }

    private void ingest(List<Document> documents) {
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);
        List<TextSegment> segments = new ArrayList<>();

        for (Document doc : documents) {
            segments.addAll(splitter.split(doc));
        }

        LOG.info("Embedding " + segments.size() + " segments...");
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        embeddingStore.addAll(embeddings, segments);
        LOG.info("Stored " + embeddings.size() + " embeddings in PgVector.");
    }
}
