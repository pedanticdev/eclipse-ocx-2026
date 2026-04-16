package com.azul.eclipseocx2026.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorSearchTest {

    @Test
    void identicalVectors_cosineSimilarityIsOne() {
        float[] v = {1.0f, 0.0f, 0.0f};
        assertEquals(1.0, VectorSearch.cosineSimilarity(v, v), 0.0001);
    }

    @Test
    void orthogonalVectors_cosineSimilarityIsZero() {
        float[] a = {1.0f, 0.0f};
        float[] b = {0.0f, 1.0f};
        assertEquals(0.0, VectorSearch.cosineSimilarity(a, b), 0.0001);
    }

    @Test
    void oppositeVectors_cosineSimilarityIsMinusOne() {
        float[] a = {1.0f, 0.0f};
        float[] b = {-1.0f, 0.0f};
        assertEquals(-1.0, VectorSearch.cosineSimilarity(a, b), 0.0001);
    }

    @Test
    void zeroVector_returnsZero() {
        float[] a = {0.0f, 0.0f};
        float[] b = {1.0f, 0.0f};
        assertEquals(0.0, VectorSearch.cosineSimilarity(a, b), 0.0001);
    }

    @Test
    void scaledVector_sameDirection() {
        float[] a = {1.0f, 2.0f, 3.0f};
        float[] b = {2.0f, 4.0f, 6.0f};
        assertEquals(1.0, VectorSearch.cosineSimilarity(a, b), 0.0001);
    }

    @Test
    void arbitraryVectors_correctAngle() {
        float[] a = {1.0f, 2.0f, 3.0f};
        float[] b = {4.0f, 5.0f, 6.0f};
        // cos(angle) = (4+10+18) / (sqrt(14) * sqrt(77)) = 32 / sqrt(1078) ≈ 0.9746
        double expected = 32.0 / (Math.sqrt(14.0) * Math.sqrt(77.0));
        assertEquals(expected, VectorSearch.cosineSimilarity(a, b), 0.0001);
    }
}
