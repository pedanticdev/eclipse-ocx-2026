package com.azul.eclipseocx2026.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaChatTest {

    OllamaChat chat;

    @BeforeEach
    void setUp() throws Exception {
        chat = new OllamaChat();
        setField("defaultModel", "gemma4:e2b");
        setField("availableModels", "gemma4:e2b,mistral");
        setField("currentModel", "gemma4:e2b");
    }

    @Test
    void getCurrentModel_returnsDefault() {
        assertEquals("gemma4:e2b", chat.getCurrentModel());
    }

    @Test
    void switchModel_updatesCurrentModel() {
        chat.switchModel("mistral");
        assertEquals("mistral", chat.getCurrentModel());
    }

    @Test
    void getAvailableModels_parsesCommaSeparated() {
        var models = chat.getAvailableModels();
        assertEquals(2, models.size());
        assertTrue(models.contains("gemma4:e2b"));
        assertTrue(models.contains("mistral"));
    }

    private void setField(String name, Object value) throws Exception {
        Field field = OllamaChat.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(chat, value);
    }
}
