package com.azul.eclipseocx2026.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConferenceChatServiceTest {

    @Mock ChatModel chatModel;
    @Mock ContentRetriever contentRetriever;
    @Mock ConferenceAgent agent;
    @Mock ConferenceOrchestrator orchestrator;
    @Mock JlamaChatModelProducer jlamaProducer;
    @Mock ConferenceAssistant assistant;

    @InjectMocks ConferenceChatService service;

    @BeforeEach
    void injectAssistant() throws Exception {
        Field field = ConferenceChatService.class.getDeclaredField("assistant");
        field.setAccessible(true);
        field.set(service, assistant);
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello", "hi", "hey", "good morning", "howdy", "greetings"})
    void greeting_callsChatModelDirectly_bypassesRag(String greeting) {
        when(chatModel.chat(any(UserMessage.class)))
                .thenReturn(chatResponse("Welcome! Ask me about Jakarta EE."));

        String result = service.ask(greeting, "declarative");

        assertTrue(result.contains("Welcome"));
        verify(assistant, never()).ask(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"What is CDI?", "Explain JAX-RS filters", "How does JPA work?"})
    void nonGreeting_delegatesToAssistant(String question) {
        when(assistant.ask(question)).thenReturn("CDI is dependency injection.");

        String result = service.ask(question, "declarative");

        assertEquals("CDI is dependency injection.", result);
        verify(chatModel, never()).chat(any(UserMessage.class));
    }

    @Test
    void agentMode_delegatesToAgent() {
        when(agent.ask("test question")).thenReturn("agent response");

        String result = service.ask("test question", "agent");

        assertEquals("agent response", result);
        verify(agent).ask("test question");
    }

    @Test
    void orchestratedMode_delegatesToOrchestrator() {
        when(orchestrator.ask("test question")).thenReturn("orchestrated response");

        String result = service.ask("test question", "orchestrated");

        assertEquals("orchestrated response", result);
        verify(orchestrator).ask("test question");
    }

    @Test
    void inProcessMode_whenAvailable_returnsResponse() {
        ChatModel jlamaModel = mock(ChatModel.class);
        when(jlamaProducer.isAvailable()).thenReturn(true);
        when(jlamaProducer.getChatModel()).thenReturn(jlamaModel);
        when(jlamaModel.chat(any(UserMessage.class)))
                .thenReturn(chatResponse("JPA is Jakarta Persistence."));

        String result = service.ask("What is JPA?", "in-process");

        assertTrue(result.contains("Jakarta Persistence"));
    }

    @Test
    void inProcessMode_whenUnavailable_returnsUnavailableMessage() {
        when(jlamaProducer.isAvailable()).thenReturn(false);

        String result = service.ask("What is JPA?", "in-process");

        assertTrue(result.contains("not available"));
    }

    @Test
    void unknownMode_defaultsToDeclarative() {
        when(assistant.ask("test")).thenReturn("default response");

        String result = service.ask("test", "anything-else");

        assertEquals("default response", result);
    }

    @Test
    void fallback_returnsServiceUnavailableMessage() {
        String result = service.fallback("test", "agent");
        assertTrue(result.contains("temporarily unavailable"));
    }

    private static ChatResponse chatResponse(String text) {
        return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
    }
}
