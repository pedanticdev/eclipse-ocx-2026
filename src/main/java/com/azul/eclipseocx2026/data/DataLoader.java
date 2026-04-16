package com.azul.eclipseocx2026.data;

import com.azul.eclipseocx2026.data.PrecomputedEmbeddingLoader.PrecomputedData;
import com.azul.eclipseocx2026.data.PrecomputedEmbeddingLoader.SegmentEntry;
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
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class DataLoader {

    private static final Logger LOG = Logger.getLogger(DataLoader.class.getName());

    private static final String[] SPEC_NAMES = {"cdi", "jpa", "concurrency", "data", "jaxrs"};
    private static final int EMBED_BATCH_SIZE = 50;

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

        List<DocumentEntity> entities = new ArrayList<>();
        List<TextSegment> allSegments = new ArrayList<>();
        List<Embedding> allEmbeddings = new ArrayList<>();

        for (String specName : SPEC_NAMES) {
            LOG.info("Loading spec: " + specName);

            PrecomputedData precomputed = PrecomputedEmbeddingLoader
                    .load("data/embeddings/" + specName + ".json");

            if (precomputed != null) {
                persistSpec(entities, specName, concatSegmentTexts(precomputed.segments()));
                for (SegmentEntry entry : precomputed.segments()) {
                    allSegments.add(TextSegment.from(entry.text()));
                    allEmbeddings.add(new Embedding(entry.embedding()));
                }
            } else {
                LOG.info("No pre-computed embeddings for " + specName + ", falling back to HTML parsing");
                loadFromHtml(specName, entities, allSegments, allEmbeddings);
            }
        }

        if (allEmbeddings.isEmpty()) {
            LOG.warning("No segments to ingest.");
            return;
        }

        embeddingStore.addAll(allEmbeddings, allSegments);
        LOG.info("Data ingestion complete. " + entities.size() + " specs, "
                + allEmbeddings.size() + " segments stored.");
    }

    private void loadFromHtml(String specName, List<DocumentEntity> entities,
                              List<TextSegment> segments, List<Embedding> embeddings) {
        String html = readResource("data/jakarta-specs/" + specName + ".html");
        if (html == null) {
            LOG.warning("HTML spec not found: " + specName);
            return;
        }

        String text = Jsoup.clean(html, Safelist.relaxed())
                .replaceAll("(?m)\\s{3,}", "\n\n").trim();

        persistSpec(entities, specName, text);

        var splitter = DocumentSplitters.recursive(1000, 200);
        List<TextSegment> specSegments = splitter.split(
                dev.langchain4j.data.document.Document.from(text));

        LOG.info("  Embedding " + specSegments.size() + " segments for " + specName
                + " in batches of " + EMBED_BATCH_SIZE + "...");

        for (int i = 0; i < specSegments.size(); i += EMBED_BATCH_SIZE) {
            int end = Math.min(i + EMBED_BATCH_SIZE, specSegments.size());
            List<TextSegment> batch = specSegments.subList(i, end);
            LOG.info("    batch " + (i / EMBED_BATCH_SIZE + 1) + ": segments " + i + "-" + (end - 1));

            List<Embedding> batchEmbeddings = embeddingModel.embedAll(batch).content();
            segments.addAll(batch);
            embeddings.addAll(batchEmbeddings);
        }
    }

    private void persistSpec(List<DocumentEntity> entities, String title, String content) {
        DocumentEntity entity = new DocumentEntity(title, "jakarta-specs", content);
        em.persist(entity);
        entities.add(entity);
    }

    private String readResource(String path) {
        try (InputStream is = getClass().getResourceAsStream("/" + path)) {
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.warning("Failed to read resource " + path + ": " + e.getMessage());
            return null;
        }
    }

    private String concatSegmentTexts(List<SegmentEntry> segments) {
        StringBuilder sb = new StringBuilder();
        for (SegmentEntry entry : segments) {
            sb.append(entry.text()).append("\n\n");
        }
        return sb.toString().trim();
    }
}
