package com.azul.eclipseocx2026.ai;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.time.Duration;

@ApplicationScoped
public class EmbeddingProducer {

    @Produces
    @ApplicationScoped
    public EmbeddingModel embeddingModel() {
        Config config = ConfigProvider.getConfig();
        String baseUrl = config.getValue("ollama.base.url", String.class);
        String modelName = config.getValue("ollama.embedding.model", String.class);

        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(120))
                .build();
    }
}
