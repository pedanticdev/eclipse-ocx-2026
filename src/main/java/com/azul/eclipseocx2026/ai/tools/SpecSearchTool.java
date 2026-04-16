package com.azul.eclipseocx2026.ai.tools;

import ai.koog.agents.core.tools.annotations.LLMDescription;
import ai.koog.agents.core.tools.annotations.Tool;
import ai.koog.agents.core.tools.reflect.ToolSet;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.util.stream.Collectors;

/**
 * Koog ToolSet that searches Jakarta EE specification documents
 * using the existing LangChain4j ContentRetriever (PgVector RAG).
 */
public class SpecSearchTool implements ToolSet {

    private final ContentRetriever contentRetriever;

    public SpecSearchTool(ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }

    @Tool
    @LLMDescription("Search Jakarta EE specification documents for relevant information about APIs, patterns, and concepts")
    public String searchSpecs(@LLMDescription("The search query about Jakarta EE specifications") String query) {
        var contents = contentRetriever.retrieve(Query.from(query));
        if (contents.isEmpty()) {
            return "No relevant specification documents found for: " + query;
        }
        return contents.stream()
                .map(Content::textSegment)
                .map(segment -> segment.text())
                .collect(Collectors.joining("\n\n"));
    }
}
