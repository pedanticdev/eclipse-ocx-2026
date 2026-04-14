package com.azul.eclipseocx2026.ai;

import com.azul.eclipseocx2026.config.VirtualThreadExecutor;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ApplicationScoped
public class BenchmarkService {

    private static final Logger LOG = Logger.getLogger(BenchmarkService.class.getName());

    @Inject
    private AiService aiService;

    @Inject
    @VirtualThreadExecutor
    private ManagedExecutorService executorService;

    public BenchmarkResult run(List<String> questions) {
        LOG.info("Starting benchmark with " + questions.size() + " questions...");

        Instant wallStart = Instant.now();

        List<CompletableFuture<QuestionResult>> futures = questions.stream()
                .map(question -> CompletableFuture.supplyAsync(
                        () -> askQuestion(question), executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        List<QuestionResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        Duration wallTime = Duration.between(wallStart, Instant.now());
        long avgMs = (long) results.stream()
                .mapToLong(QuestionResult::durationMs)
                .average()
                .orElse(0);
        long minMs = results.stream()
                .mapToLong(QuestionResult::durationMs)
                .min()
                .orElse(0);
        long maxMs = results.stream()
                .mapToLong(QuestionResult::durationMs)
                .max()
                .orElse(0);

        double throughput = wallTime.toMillis() > 0
                ? (double) questions.size() / wallTime.toMillis() * 1000
                : 0;

        LOG.info(String.format("Benchmark complete: %d questions in %dms (avg %dms, %.1f req/s)",
                questions.size(), wallTime.toMillis(), avgMs, throughput));

        return new BenchmarkResult(
                questions.size(),
                wallTime.toMillis(),
                avgMs,
                minMs,
                maxMs,
                throughput,
                results
        );
    }

    private QuestionResult askQuestion(String question) {
        Instant start = Instant.now();
        try {
            String answer = aiService.ask(question);
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            return new QuestionResult(question, durationMs, answer, null);
        } catch (Exception e) {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            return new QuestionResult(question, durationMs, null, e.getMessage());
        }
    }

    public record BenchmarkResult(
            int totalQuestions,
            long wallTimeMs,
            long avgMs,
            long minMs,
            long maxMs,
            double throughputPerSecond,
            List<QuestionResult> results
    ) {}

    public record QuestionResult(
            String question,
            long durationMs,
            String answer,
            String error
    ) {}
}
