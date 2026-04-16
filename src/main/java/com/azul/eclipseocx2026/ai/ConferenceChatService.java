package com.azul.eclipseocx2026.ai;

import com.azul.eclipseocx2026.security.Audited;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.time.temporal.ChronoUnit;
import java.util.Set;

@ApplicationScoped
public class ConferenceChatService {

    private static final Set<String> GREETINGS = Set.of(
            "hello", "hi", "hey", "good morning", "good afternoon", "good evening",
            "howdy", "greetings", "sup", "yo", "hola"
    );

    @Inject
    private ChatModel chatModel;

    @Inject
    private ContentRetriever contentRetriever;

    @Inject
    private ConferenceAgent agent;

    @Inject
    private ConferenceOrchestrator orchestrator;

    @Inject
    private JlamaChatModelProducer jlamaProducer;

    private ConferenceAssistant assistant;

    @PostConstruct
    void init() {
        assistant = AiServices.builder(ConferenceAssistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();
    }

    @Audited
    @Retry(maxRetries = 2, delay = 1, delayUnit = ChronoUnit.SECONDS)
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.6, delay = 30, delayUnit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "fallback")
    public String ask(String question, String mode) {
        return switch (mode) {
            case "agent" -> agent.ask(question);
            case "orchestrated" -> orchestrator.ask(question);
            case "in-process" -> askInProcess(question);
            default -> askDeclarative(question);
        };
    }

    String fallback(String question, String mode) {
        return "The AI service is temporarily unavailable. Please try again in a moment.";
    }

    private String askDeclarative(String question) {
        if (isGreeting(question)) {
            ChatResponse response = chatModel.chat(UserMessage.from(
                    "Respond briefly and warmly. You are a Jakarta EE expert assistant. "
                            + "Invite the user to ask about Jakarta EE specifications and APIs."
            ));
            return response.aiMessage().text();
        }
        return assistant.ask(question);
    }

    private boolean isGreeting(String input) {
        String normalized = input.trim().toLowerCase().replaceAll("[!.?]", "");
        return normalized.length() <= 30 && GREETINGS.contains(normalized);
    }

    private String askInProcess(String question) {
        if (!jlamaProducer.isAvailable()) {
            return "In-process inference is not available. Ensure the Jlama model is downloaded "
                    + "and the JVM supports the Panama Vector API (`--add-modules=jdk.incubator.vector`).";
        }
        ChatResponse response = jlamaProducer.getChatModel().chat(UserMessage.from(
                "You are a Jakarta EE expert assistant. Answer concisely.\n\n" + question
        ));
        return response.aiMessage().text();
    }
}
