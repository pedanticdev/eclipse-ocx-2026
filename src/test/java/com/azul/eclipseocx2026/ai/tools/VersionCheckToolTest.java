package com.azul.eclipseocx2026.ai.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionCheckToolTest {

    private final VersionCheckTool tool = new VersionCheckTool();

    @ParameterizedTest
    @CsvSource({
        "cdi, CDI 4.1",
        "jpa, Jakarta Persistence 3.2",
        "jax-rs, Jakarta RESTful Web Services 4.0",
        "data, Jakarta Data 1.0",
        "concurrency, Jakarta Concurrency 3.1",
        "security, Jakarta Security 4.0",
        "servlet, Jakarta Servlet 6.1"
    })
    void exactMatch_returnsCorrectVersion(String input, String expected) {
        String result = tool.checkVersion(input);
        assertTrue(result.contains(expected),
                "Expected '" + expected + "' in result for input '" + input + "'");
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertEquals(tool.checkVersion("cdi"), tool.checkVersion("CDI"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cdi events", "about jpa", "jax-rs filters", "virtual threads concurrency"})
    void fuzzyMatch_returnsRelevantData(String input) {
        String result = tool.checkVersion(input);
        assertFalse(result.startsWith("Version information not found"),
                "Fuzzy match should find data for: " + input);
    }

    @Test
    void unknownFeature_returnsNotFoundWithKnownSpecs() {
        String result = tool.checkVersion("spring");
        assertTrue(result.startsWith("Version information not found"));
        assertTrue(result.contains("Known specifications:"));
        assertTrue(result.contains("cdi"));
    }

    @Test
    void trimmedWhitespace_handled() {
        assertTrue(tool.checkVersion("  cdi  ").contains("CDI 4.1"));
    }
}
