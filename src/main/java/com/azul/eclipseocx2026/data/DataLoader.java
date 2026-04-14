package com.azul.eclipseocx2026.data;

import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class DataLoader {

    private static final Logger LOG = Logger.getLogger(DataLoader.class.getName());

    private static final String[] DOC_FILES = {"cdi.txt", "jpa.txt", "concurrency.txt", "data.txt", "jaxrs.txt"};

    @PersistenceContext
    private EntityManager em;

    @Inject
    private EmbeddingModel embeddingModel;

    @Inject
    private EmbeddingStore<TextSegment> embeddingStore;

    @Transactional
    public void init(@Observes @Initialized(ApplicationScoped.class) Object event) {
        LOG.info("Starting data ingestion...");

        long existing = em.createQuery("SELECT COUNT(d) FROM DocumentEntity d", Long.class).getSingleResult();
        if (existing > 0) {
            LOG.info("Documents already exist (" + existing + "). Skipping ingestion.");
            return;
        }

        List<DocumentEntity> entities = persistDocuments();
        if (entities.isEmpty()) {
            LOG.warning("No documents found to ingest.");
            return;
        }

        embedAndStore(entities);

        LOG.info("Data ingestion complete. " + entities.size() + " documents persisted and embedded.");
    }

    private List<DocumentEntity> persistDocuments() {
        DocumentParser parser = new TextDocumentParser();
        List<DocumentEntity> entities = new ArrayList<>();

        for (String filename : DOC_FILES) {
            InputStream is = getClass().getResourceAsStream("/data/jakarta-docs/" + filename);
            if (is != null) {
                String content = parser.parse(is).text();
                String title = filename.replace(".txt", "");
                DocumentEntity entity = new DocumentEntity(title, "jakarta-docs", content);
                em.persist(entity);
                entities.add(entity);
            }
        }

        return entities;
    }

    private void embedAndStore(List<DocumentEntity> entities) {
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);
        List<TextSegment> segments = new ArrayList<>();

        for (DocumentEntity entity : entities) {
            dev.langchain4j.data.document.Document doc = dev.langchain4j.data.document.Document.from(entity.getContent());
            segments.addAll(splitter.split(doc));
        }

        LOG.info("Embedding " + segments.size() + " segments...");
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);

        LOG.info("Stored " + embeddings.size() + " embeddings in PgVector.");
    }
}
