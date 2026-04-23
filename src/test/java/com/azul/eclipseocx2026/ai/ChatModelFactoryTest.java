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
        setField("defaultModelName", "gemma4:e2b");
        setField("availableModels", "gemma4:e2b,mistral");
        setField("currentModelName", "gemma4:e2b");
    }

    @Test
    void getCurrentModelName_returnsDefault() {
        assertEquals("gemma4:e2b", factory.getCurrentModelName());
    }

    @Test
    void getAvailableModels_parsesCommaSeparated() {
        var models = factory.getAvailableModels();
        assertEquals(2, models.size());
        assertTrue(models.contains("gemma4:e2b"));
        assertTrue(models.contains("mistral"));
    }

    private void setField(String name, Object value) throws Exception {
        Field field = ChatModelFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(factory, value);
    }
}
