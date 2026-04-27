package com.azul.eclipseocx2026.ai;

import com.azul.eclipseocx2026.model.DocumentChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock OllamaChat ollamaChat;
    @Mock VectorSearch vectorSearch;

    @InjectMocks AiService service;

    @Test
    void noRelevantChunks_returnsNotFoundMessage() {
        when(vectorSearch.search(anyString(), anyInt())).thenReturn(Collections.emptyList());

        String result = service.ask("What is CDI?");

        assertTrue(result.contains("couldn't find"));
        verify(ollamaChat, never()).chat(anyString());
    }

    @Test
    void withRelevantChunks_callsChatWithContext() {
        DocumentChunk chunk = new DocumentChunk("CDI is dependency injection.", "cdi.txt", "CDI Overview");
        when(vectorSearch.search("What is CDI?", 10)).thenReturn(List.of(chunk));
        when(ollamaChat.chat(anyString())).thenReturn("CDI provides typesafe dependency injection.");

        String result = service.ask("What is CDI?");

        assertTrue(result.contains("dependency injection"));
        verify(ollamaChat).chat(anyString());
    }

    @Test
    void chatFailure_returnsErrorMessage() {
        DocumentChunk chunk = new DocumentChunk("Some content", "src", "title");
        when(vectorSearch.search("test", 10)).thenReturn(List.of(chunk));
        when(ollamaChat.chat(anyString()))
                .thenThrow(new IllegalStateException("Connection refused"));

        String result = service.ask("test");

        assertTrue(result.contains("couldn't process"));
        assertTrue(result.contains("Connection refused"));
    }
}
