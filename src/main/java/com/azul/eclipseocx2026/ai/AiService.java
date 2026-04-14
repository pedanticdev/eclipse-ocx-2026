package com.azul.eclipseocx2026.ai;

import com.azul.eclipseocx2026.model.DocumentChunk;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class AiService {

    @Inject
    private ChatModelFactory modelFactory;

    @Inject
    private VectorSearch vectorSearch;

    private static final String SYSTEM_PROMPT = """
            You are an assistant for the Eclipse OCX 2026 conference in Brussels.
            Answer questions about conference talks, speakers, and Jakarta EE topics
            using the provided context. If unsure, say so. Be concise.
            """;

    public String ask(String question) {
        List<DocumentChunk> relevantChunks = vectorSearch.search(question, 5);

        String context = relevantChunks.stream()
                .map(chunk -> "Source: " + chunk.getSource() + "\n" + chunk.getContent())
                .collect(Collectors.joining("\n\n"));

        String userMessage = """
                Context:
                %s

                Question: %s
                """.formatted(context, question);

        ChatResponse response = modelFactory.getChatModel().chat(
                ChatRequest.builder()
                        .messages(UserMessage.from(userMessage))
                        .build());

        return response.aiMessage().text();
    }
}
