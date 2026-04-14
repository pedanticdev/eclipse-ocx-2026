package com.azul.eclipseocx2026.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class ChatModelFactory {

    private volatile ChatModel chatModel;
    private volatile String currentModelName;

    @PostConstruct
    void init() {
        Config config = ConfigProvider.getConfig();
        String modelName = config.getValue("ollama.chat.model", String.class);
        this.chatModel = createChatModel(modelName);
        this.currentModelName = modelName;
    }

    public ChatModel getChatModel() {
        return chatModel;
    }

    public String getCurrentModelName() {
        return currentModelName;
    }

    public List<String> getAvailableModels() {
        Config config = ConfigProvider.getConfig();
        String models = config.getOptionalValue("ollama.models.available", String.class)
                .orElse("llama3.2");
        return List.of(models.split(","));
    }

    public void switchModel(String modelName) {
        this.chatModel = createChatModel(modelName);
        this.currentModelName = modelName;
    }

    private ChatModel createChatModel(String modelName) {
        Config config = ConfigProvider.getConfig();
        String baseUrl = config.getValue("ollama.base.url", String.class);
        double temperature = config.getOptionalValue("ollama.chat.temperature", Double.class)
                .orElse(0.7);
        int timeout = config.getOptionalValue("ollama.chat.timeout", Integer.class)
                .orElse(300);

        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeout))
                .build();
    }
}
