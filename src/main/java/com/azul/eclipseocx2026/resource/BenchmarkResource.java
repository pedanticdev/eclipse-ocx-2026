package com.azul.eclipseocx2026.resource;

import com.azul.eclipseocx2026.ai.BenchmarkService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML;

@Path("/benchmark")
public class BenchmarkResource {

    private static final Map<String, List<String>> PRESETS = Map.of(
            "5 Questions", List.of(
                    "What talks are about virtual threads?",
                    "Who is speaking about Jakarta Data?",
                    "What is CDI?",
                    "Tell me about the JPA talks",
                    "What is the schedule for Wednesday?"
            ),
            "10 Questions", List.of(
                    "What talks are about virtual threads?",
                    "Who is speaking about Jakarta Data?",
                    "What is CDI?",
                    "Tell me about the JPA talks",
                    "What is the schedule for Wednesday?",
                    "Which talks cover cloud deployment?",
                    "What is Jakarta Concurrency?",
                    "Who works at Payara?",
                    "What is JAX-RS used for?",
                    "Are there any talks about testing?"
            ),
            "20 Questions", List.of(
                    "What talks are about virtual threads?",
                    "Who is speaking about Jakarta Data?",
                    "What is CDI?",
                    "Tell me about the JPA talks",
                    "What is the schedule for Wednesday?",
                    "Which talks cover cloud deployment?",
                    "What is Jakarta Concurrency?",
                    "Who works at Payara?",
                    "What is JAX-RS used for?",
                    "Are there any talks about testing?",
                    "What is the difference between JPA and Jakarta Data?",
                    "Who is the keynote speaker?",
                    "What talks are in the Architecture track?",
                    "Explain virtual thread caveats",
                    "What is the connection between CDI and Jakarta EE?",
                    "Tell me about REST API development",
                    "What companies are presenting?",
                    "What is the role of MicroProfile Config?",
                    "How does Jakarta Data handle queries?",
                    "What time slots are available?"
            )
    );

    @Inject
    private BenchmarkService benchmarkService;

    @GET
    @Produces(APPLICATION_JSON)
    public Map<String, List<String>> listPresets() {
        return PRESETS;
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_HTML)
    public String runBenchmark(@FormParam("preset") String presetName) {
        List<String> questions = PRESETS.getOrDefault(presetName, PRESETS.get("5 Questions"));
        BenchmarkService.BenchmarkResult result = benchmarkService.run(questions);
        return renderResults(result, presetName);
    }

    private String renderResults(BenchmarkService.BenchmarkResult result, String presetName) {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < result.results().size(); i++) {
            BenchmarkService.QuestionResult qr = result.results().get(i);
            String statusClass = qr.error() != null ? "danger" : "success";
            String status = qr.error() != null ? "FAIL" : "OK";
            String answerPreview = qr.answer() != null
                    ? qr.answer().substring(0, Math.min(80, qr.answer().length())) + "..."
                    : qr.error();
            rows.append("""
                    <tr>
                        <td>%d</td>
                        <td class="left-align">%s</td>
                        <td class="%s">%s</td>
                        <td class="%s">%s</td>
                    </tr>""".formatted(i + 1,
                    escapeHtml(qr.question()),
                    statusClass, formatDuration(qr.durationMs()),
                    statusClass, escapeHtml(answerPreview)));
        }

        return """
                <div id="benchmark-results" class="benchmark-results">
                    <div class="benchmark-summary">
                        <div class="summary-grid">
                            <div class="summary-item">
                                <div class="summary-value">%d</div>
                                <div class="summary-label">Questions</div>
                            </div>
                            <div class="summary-item">
                                <div class="summary-value">%s</div>
                                <div class="summary-label">Total Time</div>
                            </div>
                            <div class="summary-item">
                                <div class="summary-value">%s</div>
                                <div class="summary-label">Avg Latency</div>
                            </div>
                            <div class="summary-item">
                                <div class="summary-value">%s / %s</div>
                                <div class="summary-label">Min / Max</div>
                            </div>
                            <div class="summary-item">
                                <div class="summary-value">%.1f</div>
                                <div class="summary-label">Req/sec</div>
                            </div>
                        </div>
                    </div>
                    <table>
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Question</th>
                                <th>Latency</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>""".formatted(
                result.totalQuestions(),
                formatDuration(result.wallTimeMs()),
                formatDuration(result.avgMs()),
                formatDuration(result.minMs()),
                formatDuration(result.maxMs()),
                result.throughputPerSecond(),
                rows);
    }

    static String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        return String.format("%.1fs", ms / 1000.0);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
