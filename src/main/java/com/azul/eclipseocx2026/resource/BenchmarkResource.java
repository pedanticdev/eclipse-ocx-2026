package com.azul.eclipseocx2026.resource;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.message.UserMessage;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Path("/benchmark")
public class BenchmarkResource {

    public record BenchmarkResult(String hardware, long embeddingMs, long chatMs, double tokensPerSec) {}
    public record BenchmarkData(BenchmarkResult cpu, BenchmarkResult gpu) {}

    private static final Jsonb JSONB = JsonbBuilder.create();

    private static final List<String> SAMPLE_TEXTS = List.of(
            "Jakarta Contexts and Dependency Injection (CDI) provides a powerful programming model for Java applications.",
            "Jakarta Persistence provides object-relational mapping for Java applications using JPA annotations.",
            "Virtual threads in Java 21 enable lightweight concurrency without reactive programming frameworks.",
            "Jakarta Data introduces a standardized repository abstraction for data access in enterprise Java.",
            "Jakarta RESTful Web Services provides a standard API for building RESTful endpoints in Java."
    );

    private static final String BENCHMARK_PROMPT =
            "Explain what CDI is in Jakarta EE in exactly 50 words.";

    @Inject
    private EmbeddingModel embeddingModel;

    @Inject
    private ChatModel chatModel;

    @GET
    @Path("/run")
    @Produces(MediaType.APPLICATION_JSON)
    public String run() {
        List<TextSegment> segments = SAMPLE_TEXTS.stream()
                .map(TextSegment::from)
                .toList();

        long embStart = System.nanoTime();
        embeddingModel.embedAll(segments);
        long embeddingMs = (System.nanoTime() - embStart) / 1_000_000;

        long chatStart = System.nanoTime();
        ChatResponse response = chatModel.chat(UserMessage.from(BENCHMARK_PROMPT));
        long chatMs = (System.nanoTime() - chatStart) / 1_000_000;

        int wordCount = response.aiMessage().text().split("\\s+").length;
        double tokensPerSec = (wordCount / (double) chatMs) * 1000;

        return JSONB.toJson(new BenchmarkResult("fill-me-in", embeddingMs, chatMs, tokensPerSec));
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String results() {
        BenchmarkData data = loadStatic();
        if (data == null) {
            return "<div class='benchmark-error'>No benchmark data. Run <code>./run.sh benchmark</code> first.</div>";
        }

        BenchmarkResult cpu = data.cpu();
        BenchmarkResult gpu = data.gpu();

        boolean gpuReady = gpu.embeddingMs() > 0 && gpu.chatMs() > 0;

        String embSpeedup = gpuReady ? cpu.embeddingMs() / gpu.embeddingMs() + "x" : "TBD";
        String chatSpeedup = gpuReady ? cpu.chatMs() / gpu.chatMs() + "x" : "TBD";
        String tpsSpeedup = gpuReady ? String.format("%.0fx", gpu.tokensPerSec() / cpu.tokensPerSec()) : "TBD";

        String gpuEmbDisplay = gpuReady ? gpu.embeddingMs() + " ms" : "Run on GPU hardware";
        String gpuChatDisplay = gpuReady ? gpu.chatMs() + " ms" : "Run on GPU hardware";
        String gpuTpsDisplay = gpuReady ? String.format("%.0f", gpu.tokensPerSec()) : "-";

        return """
                <div class="benchmark-table">
                  <table>
                    <thead>
                      <tr><th></th><th>CPU (Dev)</th><th>GPU (Prod)</th><th>Speedup</th></tr>
                    </thead>
                    <tbody>
                      <tr class="hardware">
                        <td>Hardware</td><td>%s</td><td>%s</td><td></td>
                      </tr>
                      <tr>
                        <td>Embedding (5 segments)</td><td>%d ms</td><td>%s</td><td class="speedup">%s</td>
                      </tr>
                      <tr>
                        <td>Chat (50 words)</td><td>%d ms</td><td>%s</td><td class="speedup">%s</td>
                      </tr>
                      <tr>
                        <td>Tokens/sec</td><td>%.0f</td><td>%s</td><td class="speedup">%s</td>
                      </tr>
                    </tbody>
                  </table>
                  <p class="benchmark-note">Same WAR. Zero code changes. Only OLLAMA_BASE_URL changed.</p>
                </div>
                """.formatted(
                cpu.hardware(), gpu.hardware(),
                cpu.embeddingMs(), gpuEmbDisplay, embSpeedup,
                cpu.chatMs(), gpuChatDisplay, chatSpeedup,
                cpu.tokensPerSec(), gpuTpsDisplay, tpsSpeedup
        );
    }

    private BenchmarkData loadStatic() {
        try (InputStream is = getClass().getResourceAsStream("/benchmark-results.json")) {
            if (is == null) return null;
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return JSONB.fromJson(json, BenchmarkData.class);
        } catch (Exception e) {
            return null;
        }
    }
}
