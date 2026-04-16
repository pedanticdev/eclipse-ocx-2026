package com.azul.eclipseocx2026.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatModelFactoryTest {

    ChatModelFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = new ChatModelFactory();
        setField("defaultModelName", "gemma3:4b");
        setField("availableModels", "gemma3:4b,mistral");
        setField("currentModelName", "gemma3:4b");
    }

    @Test
    void getCurrentModelName_returnsDefault() {
        assertEquals("gemma3:4b", factory.getCurrentModelName());
    }

    @Test
    void getAvailableModels_parsesCommaSeparated() {
        var models = factory.getAvailableModels();
        assertEquals(2, models.size());
        assertTrue(models.contains("gemma3:4b"));
        assertTrue(models.contains("mistral"));
    }

    private void setField(String name, Object value) throws Exception {
        Field field = ChatModelFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(factory, value);
    }
}
