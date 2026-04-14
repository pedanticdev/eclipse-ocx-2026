package com.azul.eclipseocx2026.ai;

import com.azul.eclipseocx2026.model.DocumentChunk;
import com.azul.eclipseocx2026.repository.Chunks;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class VectorSearch {

    @Inject
    private Chunks chunks;

    @Inject
    private EmbeddingModel embeddingModel;

    public List<DocumentChunk> search(String query, int maxResults) {
        float[] queryEmbedding = embeddingModel.embed(query).content().vector();

        List<DocumentChunk> allChunks = chunks.findAll().toList();

        return allChunks.stream()
                .filter(chunk -> chunk.getEmbedding() != null)
                .map(chunk -> new SimilarityResult(chunk,
                        cosineSimilarity(queryEmbedding, chunk.getEmbeddingVector())))
                .sorted(Comparator.comparingDouble(SimilarityResult::score).reversed())
                .limit(maxResults)
                .map(SimilarityResult::chunk)
                .toList();
    }

    static double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private record SimilarityResult(DocumentChunk chunk, double score) {}
}
