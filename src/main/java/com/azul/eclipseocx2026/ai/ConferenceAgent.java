package com.azul.eclipseocx2026.ai;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Agentic workflow using LangGraph4j.
 *
 * Graph: START -> classify -> (rag | direct) -> respond -> END
 *
 * The LLM classifies each query to decide whether RAG retrieval is needed
 * (specification questions) or whether a direct LLM answer suffices
 * (greetings, out-of-domain questions).
 */
@ApplicationScoped
public class ConferenceAgent {

    private static final Logger LOG = Logger.getLogger(ConferenceAgent.class.getName());

    @Inject
    ChatModel chatModel;

    @Inject
    ContentRetriever contentRetriever;

    private CompiledGraph<AgentState> workflow;

    @PostConstruct
    void init() {
        try {
            Map<String, Channel<?>> schema = Map.of(
                    "query", Channels.base(() -> ""),
                    "classification", Channels.base(() -> ""),
                    "context", Channels.base(() -> ""),
                    "response", Channels.base(() -> "")
            );

            this.workflow = new StateGraph<>(schema, AgentState::new)
                    .addNode("classify", AsyncNodeAction.node_async(classifyNode()))
                    .addNode("rag", AsyncNodeAction.node_async(ragNode()))
                    .addNode("direct", AsyncNodeAction.node_async(directNode()))
                    .addNode("respond", AsyncNodeAction.node_async(respondNode()))
                    .addEdge(StateGraph.START, "classify")
                    .addConditionalEdges(
                            "classify",
                            AsyncEdgeAction.edge_async(routeByClassification()),
                            Map.of("rag", "rag", "direct", "direct")
                    )
                    .addEdge("rag", "respond")
                    .addEdge("direct", "respond")
                    .addEdge("respond", StateGraph.END)
                    .compile();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize ConferenceAgent workflow", e);
        }
    }

    public String ask(String question) {
        LOG.info("Agent received query: " + question);

        var result = workflow.invoke(Map.of("query", question));

        return result.map(state -> getString(state, "response"))
                .filter(s -> !s.isEmpty())
                .orElse("I couldn't process that.");
    }

    @SuppressWarnings("unchecked")
    private String getString(AgentState state, String key) {
        return (String) state.value(key).orElse("");
    }

    private NodeAction<AgentState> classifyNode() {
        return state -> {
            String query = getString(state, "query");

            String prompt = """
                    Classify this query into exactly one category:
                    - "rag" if it asks about Jakarta EE specifications, APIs, or Java EE concepts
                    - "direct" if it is a greeting, small talk, or unrelated to Jakarta EE

                    Query: %s

                    Respond with ONLY "rag" or "direct", nothing else.
                    """.formatted(query);

            ChatResponse response = chatModel.chat(UserMessage.from(prompt));
            String classification = response.aiMessage().text().trim().toLowerCase();

            if (!classification.equals("rag") && !classification.equals("direct")) {
                classification = "rag";
            }

            LOG.info("Classification: " + classification);
            return Map.of("classification", classification);
        };
    }

    private NodeAction<AgentState> ragNode() {
        return state -> {
            String query = getString(state, "query");

            List<Content> contents = contentRetriever.retrieve(dev.langchain4j.rag.query.Query.from(query));
            String context = contents.stream()
                    .map(content -> content.textSegment().text())
                    .collect(Collectors.joining("\n\n"));

            LOG.info("RAG retrieved " + contents.size() + " chunks");
            return Map.of("context", context);
        };
    }

    private NodeAction<AgentState> directNode() {
        return state -> Map.of("context", "");
    }

    private NodeAction<AgentState> respondNode() {
        return state -> {
            String query = getString(state, "query");
            String context = getString(state, "context");

            if (context.isEmpty()) {
                ChatResponse response = chatModel.chat(UserMessage.from(
                        "Respond briefly and warmly. You are a Jakarta EE expert assistant. "
                                + "Invite the user to ask about Jakarta EE. User said: " + query
                ));
                return Map.of("response", response.aiMessage().text());
            }

            String prompt = """
                    You are a Jakarta EE expert assistant. Answer the question using the provided context.
                    If the context doesn't contain enough information, say so. Be concise.

                    Context:
                    %s

                    Question: %s
                    """.formatted(context, query);

            ChatResponse response = chatModel.chat(UserMessage.from(prompt));
            return Map.of("response", response.aiMessage().text());
        };
    }

    private EdgeAction<AgentState> routeByClassification() {
        return state -> getString(state, "classification").isEmpty()
                ? "rag" : getString(state, "classification");
    }
}
