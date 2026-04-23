package com.azul.eclipseocx2026.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class ChatModelFactory {

    @Inject
    @ConfigProperty(name = "ollama.base.url")
    private String baseUrl;

    @Inject
    @ConfigProperty(name = "ollama.chat.model")
    private String defaultModelName;

    @Inject
    @ConfigProperty(name = "ollama.chat.temperature", defaultValue = "0.7")
    private double temperature;

    @Inject
    @ConfigProperty(name = "ollama.chat.timeout", defaultValue = "300")
    private int timeoutSeconds;

    @Inject
    @ConfigProperty(name = "ollama.models.available", defaultValue = "gemma4:e2b")
    private String availableModels;

    private volatile ChatModel chatModel;
    private volatile String currentModelName;

    @PostConstruct
    void init() {
        this.chatModel = createChatModel(defaultModelName);
        this.currentModelName = defaultModelName;
    }

    public ChatModel getChatModel() {
        return chatModel;
    }

    public String getCurrentModelName() {
        return currentModelName;
    }

    public List<String> getAvailableModels() {
        return List.of(availableModels.split(","));
    }

    public void switchModel(String modelName) {
        this.chatModel = createChatModel(modelName);
        this.currentModelName = modelName;
    }

    private ChatModel createChatModel(String modelName) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
