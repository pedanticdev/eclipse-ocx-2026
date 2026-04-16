package com.azul.eclipseocx2026.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class EmbeddingConverterTest {

    @Test
    void roundTrip_preservesValues() {
        float[] original = {1.0f, -2.5f, 3.14f, 0.0f, -0.001f};
        byte[] bytes = EmbeddingConverter.toByteArray(original);
        float[] restored = EmbeddingConverter.toFloatArray(bytes);
        assertArrayEquals(original, restored, 0.0f);
    }

    @Test
    void singleValue_roundTrip() {
        float[] original = {42.0f};
        float[] restored = EmbeddingConverter.toFloatArray(EmbeddingConverter.toByteArray(original));
        assertArrayEquals(original, restored, 0.0f);
    }

    @Test
    void emptyArray_roundTrip() {
        float[] original = {};
        byte[] bytes = EmbeddingConverter.toByteArray(original);
        assertEquals(0, bytes.length);
        float[] restored = EmbeddingConverter.toFloatArray(bytes);
        assertArrayEquals(original, restored, 0.0f);
    }

    @Test
    void byteLengthIsFourTimesFloatCount() {
        float[] values = new float[768]; // typical embedding dimension
        byte[] bytes = EmbeddingConverter.toByteArray(values);
        assertEquals(768 * Float.BYTES, bytes.length);
    }

    private static void assertEquals(int expected, int actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
