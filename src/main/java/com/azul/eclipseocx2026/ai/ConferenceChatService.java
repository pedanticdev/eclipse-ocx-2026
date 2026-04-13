package com.azul.eclipseocx2026.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

    private ConferenceAssistant assistant;

    @PostConstruct
    void init() {
        assistant = AiServices.builder(ConferenceAssistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();
    }

    public String ask(String question) {
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
}
