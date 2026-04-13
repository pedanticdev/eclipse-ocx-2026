package com.azul.eclipseocx2026.resource;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/debug")
public class DebugResource {

    @Inject
    private EmbeddingStore<TextSegment> store;

    @Inject
    private EmbeddingModel model;

    @GET
    @Path("/search")
    @Produces(MediaType.TEXT_PLAIN)
    public String search(@QueryParam("q") String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("Store type: ").append(store.getClass().getName()).append("\n");
        sb.append("Model type: ").append(model.getClass().getName()).append("\n\n");

        if (query != null && !query.isBlank()) {
            var queryEmbedding = model.embed(query).content();

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(40)
                    .minScore(0.0)
                    .build();

            EmbeddingSearchResult<TextSegment> result = store.search(request);
            List<EmbeddingMatch<TextSegment>> matches = result.matches();

            sb.append("Query: ").append(query).append("\n");
            sb.append("Matches found: ").append(matches.size()).append("\n\n");
            for (EmbeddingMatch<TextSegment> match : matches) {
                sb.append(String.format("Score: %.4f%n", match.score()));
                sb.append("Text: ").append(match.embedded().text(), 0, Math.min(200, match.embedded().text().length())).append("\n");
                sb.append("---\n");
            }
        }

        return sb.toString();
    }
}
