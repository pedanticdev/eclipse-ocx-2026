package com.azul.eclipseocx2026.data;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads pre-computed embedding JSON files produced by
 * {@link com.azul.eclipseocx2026.build.EmbeddingPreComputer}.
 * <p>
 * Uses Jakarta JSON Processing (JSON-P) directly since the format is simple
 * and this avoids any JSON-B mapping configuration for primitive arrays.
 */
public class PrecomputedEmbeddingLoader {

    private static final Logger LOG = Logger.getLogger(PrecomputedEmbeddingLoader.class.getName());

    private PrecomputedEmbeddingLoader() {}

    /**
     * Parses a pre-computed embedding JSON file from the classpath.
     *
     * @param resourcePath classpath path, e.g. "data/embeddings/cdi.json"
     * @return parsed data, or null if the resource does not exist or is invalid
     */
    public static PrecomputedData load(String resourcePath) {
        try (InputStream is = PrecomputedEmbeddingLoader.class.getResourceAsStream("/" + resourcePath)) {
            if (is == null) {
                LOG.fine("Pre-computed embeddings not found: " + resourcePath);
                return null;
            }

            try (JsonReader reader = Json.createReader(is)) {
                JsonObject root = reader.readObject();
                return parse(root);
            }
        } catch (Exception e) {
            LOG.warning("Failed to load pre-computed embeddings from " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    private static PrecomputedData parse(JsonObject root) {
        String source = root.getString("source", "unknown");
        int chunkSize = root.getInt("chunkSize", 1000);
        int overlap = root.getInt("overlap", 200);
        String modelName = root.getString("embeddingModel", "nomic-embed-text");

        JsonArray segmentsArray = root.getJsonArray("segments");
        List<SegmentEntry> segments = new ArrayList<>(segmentsArray.size());

        for (JsonValue item : segmentsArray) {
            JsonObject obj = item.asJsonObject();
            String text = obj.getString("text");
            JsonArray vec = obj.getJsonArray("embedding");
            float[] vector = new float[vec.size()];
            for (int i = 0; i < vec.size(); i++) {
                vector[i] = (float) vec.getJsonNumber(i).doubleValue();
            }
            segments.add(new SegmentEntry(text, vector));
        }

        LOG.info("Loaded pre-computed embeddings: " + source + " (" + segments.size()
                + " segments, chunkSize=" + chunkSize + ", model=" + modelName + ")");
        return new PrecomputedData(source, chunkSize, overlap, modelName, segments);
    }

    public record PrecomputedData(
            String source,
            int chunkSize,
            int overlap,
            String embeddingModel,
            List<SegmentEntry> segments
    ) {}

    public record SegmentEntry(String text, float[] embedding) {}
}
