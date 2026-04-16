package com.azul.eclipseocx2026.ai;

import com.azul.eclipseocx2026.model.DocumentChunk;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock ChatModelFactory modelFactory;
    @Mock VectorSearch vectorSearch;
    @Mock ChatModel chatModel;

    @InjectMocks AiService service;

    @Test
    void noRelevantChunks_returnsNotFoundMessage() {
        when(vectorSearch.search(anyString(), anyInt())).thenReturn(Collections.emptyList());

        String result = service.ask("What is CDI?");

        assertTrue(result.contains("couldn't find"));
        verify(modelFactory, org.mockito.Mockito.never()).getChatModel();
    }

    @Test
    void withRelevantChunks_callsLlmWithContext() {
        DocumentChunk chunk = new DocumentChunk("CDI is dependency injection.", "cdi.txt", "CDI Overview");
        when(vectorSearch.search("What is CDI?", 5)).thenReturn(List.of(chunk));
        when(modelFactory.getChatModel()).thenReturn(chatModel);
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("CDI provides typesafe dependency injection.")).build());

        String result = service.ask("What is CDI?");

        assertTrue(result.contains("dependency injection"));
        verify(chatModel).chat(any(ChatRequest.class));
    }

    @Test
    void llmFailure_returnsErrorMessage() {
        DocumentChunk chunk = new DocumentChunk("Some content", "src", "title");
        when(vectorSearch.search("test", 5)).thenReturn(List.of(chunk));
        when(modelFactory.getChatModel()).thenReturn(chatModel);
        when(chatModel.chat(any(ChatRequest.class))).thenThrow(new RuntimeException("Connection refused"));

        String result = service.ask("test");

        assertTrue(result.contains("couldn't process"));
        assertTrue(result.contains("Connection refused"));
    }
}
