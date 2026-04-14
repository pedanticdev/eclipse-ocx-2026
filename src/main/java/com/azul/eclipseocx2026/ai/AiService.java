package com.azul.eclipseocx2026.ai;

import com.azul.eclipseocx2026.model.DocumentChunk;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class AiService {

    private static final Logger LOG = Logger.getLogger(AiService.class.getName());

    @Inject
    private ChatModelFactory modelFactory;

    @Inject
    private VectorSearch vectorSearch;

    public String ask(String question) {
        try {
            List<DocumentChunk> relevantChunks = vectorSearch.search(question, 5);

            if (relevantChunks.isEmpty()) {
                return "I couldn't find any relevant information for that question. Try asking about conference talks or Jakarta EE topics.";
            }

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
        } catch (Exception e) {
            LOG.warning("AI request failed for '" + question + "': " + e.getMessage());
            return "Sorry, I couldn't process that question. The AI model may be unavailable. Error: " + e.getMessage();
        }
    }
}
