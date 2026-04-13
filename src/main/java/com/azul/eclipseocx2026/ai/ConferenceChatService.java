package com.azul.eclipseocx2026.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConferenceChatService {

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
        return assistant.ask(question);
    }
}
