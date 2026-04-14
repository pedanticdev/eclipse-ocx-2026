package com.azul.eclipseocx2026.data;

import com.azul.eclipseocx2026.config.VirtualThreadExecutor;
import com.azul.eclipseocx2026.model.ConferenceTalk;
import com.azul.eclipseocx2026.model.DocumentChunk;
import com.azul.eclipseocx2026.repository.Chunks;
import com.azul.eclipseocx2026.repository.Talks;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

@ApplicationScoped
public class DataLoader {

    private static final Logger LOG = Logger.getLogger(DataLoader.class.getName());

    private static final String[] DOC_FILES = {"cdi.txt", "jpa.txt", "concurrency.txt", "data.txt", "jaxrs.txt"};

    @Inject
    private Talks talks;

    @Inject
    private Chunks chunks;

    @Inject
    private EmbeddingModel embeddingModel;

    @Inject
    @VirtualThreadExecutor
    private ManagedExecutorService executorService;

    @Transactional
    public void init(@Observes @Initialized(ApplicationScoped.class) Object event) {
        LOG.info("Starting data ingestion...");

        long existing = chunks.countBySource("jakarta-docs");
        if (existing > 0) {
            LOG.info("Chunks already exist (" + existing + "). Skipping ingestion.");
            return;
        }

        seedConferenceTalks();

        List<DocumentChunk> docChunks = loadJakartaDocs();
        docChunks.addAll(loadTalkChunks());

        LOG.info("Generating embeddings for " + docChunks.size() + " chunks using virtual threads...");
        generateEmbeddingsConcurrently(docChunks);

        docChunks.forEach(chunks::save);

        LOG.info("Data ingestion complete. " + docChunks.size() + " chunks persisted with embeddings.");
    }

    private void seedConferenceTalks() {
        try (InputStream is = getClass().getResourceAsStream("/data/talks.json")) {
            if (is == null) {
                LOG.warning("talks.json not found. Skipping conference talk seeding.");
                return;
            }

            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JsonArray talkArray = Json.createReader(new StringReader(json)).readArray();

            for (int i = 0; i < talkArray.size(); i++) {
                JsonObject obj = talkArray.getJsonObject(i);
                ConferenceTalk talk = new ConferenceTalk(
                        obj.getString("title"),
                        obj.getString("abstractText"),
                        obj.getString("speakerName"),
                        obj.getString("speakerCompany"),
                        obj.getString("speakerBio"),
                        obj.getString("track"),
                        obj.getString("timeSlot")
                );
                talks.save(talk);
            }

            LOG.info("Seeded " + talkArray.size() + " conference talks.");
        } catch (Exception e) {
            LOG.warning("Failed to seed conference talks: " + e.getMessage());
        }
    }

    private List<DocumentChunk> loadJakartaDocs() {
        List<DocumentChunk> allChunks = new ArrayList<>();

        for (String filename : DOC_FILES) {
            try (InputStream is = getClass().getResourceAsStream("/data/jakarta-docs/" + filename)) {
                if (is == null) continue;

                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String title = filename.replace(".txt", "");

                List<String> splits = splitText(content, 300, 50);
                for (int i = 0; i < splits.size(); i++) {
                    allChunks.add(new DocumentChunk(splits.get(i), "jakarta-docs", title + "-" + i));
                }
            } catch (Exception e) {
                LOG.warning("Failed to load " + filename + ": " + e.getMessage());
            }
        }

        return allChunks;
    }

    private List<DocumentChunk> loadTalkChunks() {
        List<DocumentChunk> talkChunks = new ArrayList<>();

        talks.findAll().forEach(talk -> {
            String text = "Title: " + talk.getTitle() +
                    "\nSpeaker: " + talk.getSpeakerName() + " (" + talk.getSpeakerCompany() + ")" +
                    "\nTrack: " + talk.getTrack() +
                    "\nTime: " + talk.getTimeSlot() +
                    "\nAbstract: " + talk.getAbstractText();
            talkChunks.add(new DocumentChunk(text, "conference-talks", talk.getTitle()));
        });

        return talkChunks;
    }

    private void generateEmbeddingsConcurrently(List<DocumentChunk> documentChunks) {
        List<Future<Void>> futures = documentChunks.stream()
                .map(chunk -> executorService.submit(() -> {
                    Response<Embedding> response = embeddingModel.embed(chunk.getContent());
                    chunk.setEmbeddingVector(response.content().vector());
                    return (Void) null;
                }))
                .toList();

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                LOG.warning("Embedding generation failed: " + e.getMessage());
            }
        }
    }

    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end).trim());
            start += chunkSize - overlap;
            if (start >= text.length()) break;
        }
        return chunks;
    }
}
