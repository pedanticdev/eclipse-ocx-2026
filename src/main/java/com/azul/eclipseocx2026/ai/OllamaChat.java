package com.azul.eclipseocx2026.ai;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class OllamaChat {

    @Inject
    @ConfigProperty(name = "ollama.base.url")
    private String baseUrl;

    @Inject
    @ConfigProperty(name = "ollama.chat.model")
    private String defaultModel;

    @Inject
    @ConfigProperty(name = "ollama.chat.temperature", defaultValue = "0.7")
    private double temperature;

    @Inject
    @ConfigProperty(name = "ollama.chat.timeout", defaultValue = "300")
    private int timeoutSeconds;

    @Inject
    @ConfigProperty(name = "ollama.models.available", defaultValue = "gemma4:e2b")
    private String availableModels;

    private HttpClient http;
    private volatile String currentModel;

    @PostConstruct
    void init() {
        this.currentModel = defaultModel;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public String getCurrentModel() {
        return currentModel;
    }

    public List<String> getAvailableModels() {
        return List.of(availableModels.split(","));
    }

    public void switchModel(String modelName) {
        this.currentModel = modelName;
    }

    public String chat(String userMessage) {
        JsonObject body = Json.createObjectBuilder()
                .add("model", currentModel)
                .add("stream", false)
                .add("options", Json.createObjectBuilder()
                        .add("temperature", temperature))
                .add("messages", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("role", "user")
                                .add("content", userMessage)))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        try {
            HttpResponse<String> response = http.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                        "Ollama chat returned HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonObject result = Json.createReader(
                    new StringReader(response.body())).readObject();
            return result.getJsonObject("message").getString("content");
        } catch (IOException e) {
            throw new IllegalStateException("Chat request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chat request interrupted", e);
        }
    }
}
