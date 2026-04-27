package com.azul.eclipseocx2026.ai;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class OllamaEmbeddings {

    @Inject
    @ConfigProperty(name = "ollama.base.url")
    private String baseUrl;

    @Inject
    @ConfigProperty(name = "ollama.embedding.model")
    private String model;

    private HttpClient http;

    @PostConstruct
    void init() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public float[] embed(String text) {
        JsonObject body = Json.createObjectBuilder()
                .add("model", model)
                .add("prompt", text)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/embeddings"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        try {
            HttpResponse<String> response = http.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                        "Ollama embed returned HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonObject result = Json.createReader(
                    new StringReader(response.body())).readObject();
            JsonArray vector = result.getJsonArray("embedding");

            float[] floats = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) {
                floats[i] = (float) vector.getJsonNumber(i).doubleValue();
            }
            return floats;
        } catch (IOException e) {
            throw new IllegalStateException("Embedding request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding request interrupted", e);
        }
    }
}
