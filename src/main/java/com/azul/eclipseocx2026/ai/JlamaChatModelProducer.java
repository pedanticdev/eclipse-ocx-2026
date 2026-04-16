package com.azul.eclipseocx2026.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.jlama.JlamaChatModel;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Produces a Jlama-backed ChatModel for in-JVM LLM inference.
 *
 * Jlama runs inference using the Panama Vector API (SIMD), eliminating
 * the need for an external Ollama process. The model is loaded from
 * a local cache directory (downloaded on first use or during Docker build).
 *
 * This bean initializes gracefully: if the model isn't available or the
 * JVM doesn't support Panama Vector API, it logs a warning and reports
 * unavailable. The rest of the application (Ollama modes) works fine.
 */
@ApplicationScoped
public class JlamaChatModelProducer {

    private static final Logger LOG = Logger.getLogger(JlamaChatModelProducer.class.getName());

    @Inject
    @ConfigProperty(name = "jlama.model.name", defaultValue = "tjake/gemma-2b-it-jlama-Q4")
    String modelName;

    @Inject
    @ConfigProperty(name = "jlama.model.cache-path", defaultValue = "/opt/jlama/models")
    String modelCachePath;

    private ChatModel jlamaChatModel;
    private boolean available;

    @PostConstruct
    void init() {
        try {
            LOG.info("Loading Jlama model: " + modelName + " from " + modelCachePath);
            this.jlamaChatModel = JlamaChatModel.builder()
                    .modelName(modelName)
                    .modelCachePath(Path.of(modelCachePath))
                    .temperature(0.7f)
                    .quantizeModelAtRuntime(true)
                    .build();
            this.available = true;
            LOG.info("Jlama model loaded successfully");
        } catch (Exception e) {
            LOG.warning("Jlama in-process inference unavailable: " + e.getMessage());
            this.available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public ChatModel getChatModel() {
        return jlamaChatModel;
    }
}
