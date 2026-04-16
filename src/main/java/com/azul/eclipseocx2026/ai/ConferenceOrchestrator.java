package com.azul.eclipseocx2026.ai;

import ai.koog.agents.core.agent.AIAgent;
import ai.koog.agents.core.tools.ToolRegistry;
import ai.koog.agents.core.tools.ToolRegistryBuilder;
import ai.koog.prompt.executor.model.PromptExecutor;
import ai.koog.prompt.llm.LLMProvider;
import ai.koog.prompt.llm.LLModel;
import com.azul.eclipseocx2026.ai.tools.CodeExampleTool;
import com.azul.eclipseocx2026.ai.tools.SpecSearchTool;
import com.azul.eclipseocx2026.ai.tools.VersionCheckTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.logging.Logger;

/**
 * Agent composition using Koog.
 *
 * Creates a planner agent with three specialist tools:
 * 1. SpecSearchTool - searches Jakarta EE spec documents (RAG)
 * 2. VersionCheckTool - looks up Jakarta EE version information
 * 3. CodeExampleTool - generates code examples via the LLM
 *
 * The planner agent uses Koog's AIAgent runtime to decide which tools
 * to call based on the user's question, then synthesizes a response.
 */
@ApplicationScoped
public class ConferenceOrchestrator {

    private static final Logger LOG = Logger.getLogger(ConferenceOrchestrator.class.getName());

    @Inject
    ChatModel chatModel;

    @Inject
    ContentRetriever contentRetriever;

    @Inject
    @ConfigProperty(name = "ollama.base.url", defaultValue = "http://localhost:11434")
    String ollamaBaseUrl;

    @Inject
    @ConfigProperty(name = "ollama.model.name", defaultValue = "gemma4:e2b")
    String ollamaModelName;

    private AIAgent<String, String> plannerAgent;

    @PostConstruct
    void init() {
        LOG.info("Initializing Koog agent composition...");

        // Create Koog's PromptExecutor backed by Ollama
        PromptExecutor executor = PromptExecutor.builder()
                .ollama(ollamaBaseUrl)
                .build();

        // Define the Ollama model
        LLModel model = new LLModel(
                LLMProvider.Ollama,
                ollamaModelName,
                List.of(),
                null,
                null
        );

        // Create specialist tools
        SpecSearchTool specTool = new SpecSearchTool(contentRetriever);
        VersionCheckTool versionTool = new VersionCheckTool();
        CodeExampleTool codeTool = new CodeExampleTool(chatModel);

        // Build tool registry from ToolSets via reflection
        ToolRegistryBuilder registryBuilder = new ToolRegistryBuilder();
        registryBuilder.tools(specTool);
        registryBuilder.tools(versionTool);
        registryBuilder.tools(codeTool);
        ToolRegistry registry = registryBuilder.build();

        // Build the planner agent
        this.plannerAgent = AIAgent.builder()
                .promptExecutor(executor)
                .llmModel(model)
                .systemPrompt("""
                        You are an intelligent orchestrator for Jakarta EE questions.
                        You have access to three specialist tools:

                        1. searchSpecs - Search Jakarta EE specification documents for detailed technical information
                        2. checkVersion - Look up which Jakarta EE version introduced a specific feature or API
                        3. generateExample - Generate a concise code example for a Jakarta EE API or pattern

                        Analyze the user's question and use the appropriate tool(s) to provide a comprehensive answer.
                        You may call multiple tools in sequence if the question requires it.

                        Examples:
                        - "Explain CDI events" -> use searchSpecs
                        - "When was @DataSourceDefinition added?" -> use checkVersion
                        - "Show me a CDI producer example" -> use searchSpecs then generateExample
                        - "What's new in Jakarta EE 11 concurrency?" -> use checkVersion then searchSpecs

                        Always synthesize the tool results into a clear, well-structured response.
                        """)
                .toolRegistry(registry)
                .build();

        LOG.info("Koog planner agent initialized with 3 specialist tools");
    }

    public String ask(String question) {
        LOG.info("Koog orchestrator received query: " + question);
        try {
            return plannerAgent.run(question);
        } catch (Exception e) {
            LOG.severe("Koog agent execution failed: " + e.getMessage());
            return "Agent composition pipeline encountered an issue: " + e.getMessage();
        }
    }
}
