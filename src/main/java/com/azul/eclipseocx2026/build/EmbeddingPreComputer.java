package com.azul.eclipseocx2026.build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Build-time tool that pre-computes embeddings from Jakarta EE spec HTML files.
 * <p>
 * Run via: {@code mvn process-resources -Pprecompute-embeddings}
 * <p>
 * Requires Ollama running locally with {@code nomic-embed-text} pulled.
 */
public class EmbeddingPreComputer {

    private static final int CHUNK_SIZE = 1000;
    private static final int OVERLAP = 200;
    private static final String MODEL_NAME = "nomic-embed-text";
    private static final int DIMENSION = 768;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: EmbeddingPreComputer <inputDir> <outputDir>");
            System.exit(1);
        }

        Path inputDir = Path.of(args[0]);
        Path outputDir = Path.of(args[1]);
        Files.createDirectories(outputDir);

        if (!Files.isDirectory(inputDir)) {
            System.err.println("Input directory not found: " + inputDir);
            System.exit(1);
        }

        EmbeddingModel model = OllamaEmbeddingModel.builder()
                .baseUrl(System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"))
                .modelName(MODEL_NAME)
                .timeout(Duration.ofSeconds(120))
                .build();

        var splitter = DocumentSplitters.recursive(CHUNK_SIZE, OVERLAP);
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        List<Path> htmlFiles = listHtmlFiles(inputDir);
        System.out.printf("Found %d spec file(s) in %s%n", htmlFiles.size(), inputDir);

        for (Path htmlFile : htmlFiles) {
            String sourceName = htmlFile.getFileName().toString().replace(".html", "");
            System.out.printf("%nProcessing: %s%n", sourceName);

            String html = Files.readString(htmlFile);
            String text = extractText(html);
            System.out.printf("  Extracted %d characters%n", text.length());

            dev.langchain4j.data.document.Document doc =
                    dev.langchain4j.data.document.Document.from(text);
            List<TextSegment> segments = splitter.split(doc);
            System.out.printf("  Split into %d segments%n", segments.size());

            System.out.println("  Embedding...");
            List<Embedding> embeddings = model.embedAll(segments).content();

            List<Map<String, Object>> segmentData = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("text", segments.get(i).text());
                entry.put("embedding", embeddings.get(i).vector());
                segmentData.add(entry);
            }

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("source", sourceName);
            output.put("chunkSize", CHUNK_SIZE);
            output.put("overlap", OVERLAP);
            output.put("embeddingModel", MODEL_NAME);
            output.put("dimension", DIMENSION);
            output.put("segments", segmentData);

            Path outFile = outputDir.resolve(sourceName + ".json");
            mapper.writeValue(outFile.toFile(), output);
            System.out.printf("  Written: %s (%d bytes)%n", outFile, Files.size(outFile));
        }

        System.out.printf("%nDone. Output in %s%n", outputDir);
    }

    static String extractText(String html) {
        String cleaned = Jsoup.clean(html, Safelist.relaxed());
        return cleaned.replaceAll("(?m)\\s{3,}", "\n\n").trim();
    }

    private static List<Path> listHtmlFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.html")) {
            for (Path entry : stream) {
                files.add(entry);
            }
        }
        return files;
    }
}
