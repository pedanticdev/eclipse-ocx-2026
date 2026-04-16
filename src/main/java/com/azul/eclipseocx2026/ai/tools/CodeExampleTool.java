package com.azul.eclipseocx2026.ai.tools;

import ai.koog.agents.core.tools.annotations.LLMDescription;
import ai.koog.agents.core.tools.annotations.Tool;
import ai.koog.agents.core.tools.reflect.ToolSet;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Koog ToolSet that generates Jakarta EE code examples via the LLM.
 * Delegates to the CDI-managed ChatModel with a code-generation system prompt.
 */
public class CodeExampleTool implements ToolSet {

    private final ChatModel chatModel;

    public CodeExampleTool(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Tool
    @LLMDescription("Generate a concise code example for a Jakarta EE API, pattern, or feature")
    public String generateExample(@LLMDescription("The Jakarta EE API, pattern, or feature to demonstrate (e.g. 'CDI producer method', 'JPA entity with relationships')") String api) {
        String prompt = """
                Generate a concise, idiomatic Jakarta EE 11 code example for: %s

                Requirements:
                - Use Java 21 features where applicable (records, pattern matching, text blocks)
                - Use Jakarta EE 11 APIs and annotations (jakarta.* namespace, NOT javax.*)
                - Include brief inline comments for key parts
                - Keep the example under 30 lines
                - Show a realistic, runnable snippet
                """.formatted(api);

        ChatResponse response = chatModel.chat(UserMessage.from(prompt));
        return response.aiMessage().text();
    }
}
