package com.azul.eclipseocx2026.resource;

import com.azul.eclipseocx2026.ai.ConferenceChatService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Set;
import java.util.regex.Pattern;

@Path("/chat")
public class ChatResource {

    private static final int MAX_QUESTION_LENGTH = 500;
    private static final Set<String> ALLOWED_MODES = Set.of("declarative", "agent", "orchestrated", "in-process");
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignore\\s+(previous|all|above|prior)\\s+(instructions?|rules?|prompts?)" +
                    "|you\\s+are\\s+now\\s+a" +
                    "|system\\s*:\\s*you\\s+are)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    @Inject
    private ConferenceChatService chatService;

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public String ask(@FormParam("question") String question,
                      @FormParam("mode") @DefaultValue("declarative") String mode) {
        String validationError = validate(question, mode);
        if (validationError != null) {
            return renderAnswer(question, validationError);
        }

        String answer = chatService.ask(question, mode);
        return renderAnswer(question, answer);
    }

    /**
     * Validates user input before it reaches any LLM prompt.
     * Returns an error message on failure, null on success.
     */
    private String validate(String question, String mode) {
        if (question == null || question.isBlank()) {
            return "Please enter a question.";
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            return "Question too long. Maximum " + MAX_QUESTION_LENGTH + " characters.";
        }
        if (!ALLOWED_MODES.contains(mode)) {
            return "Unknown mode: " + escapeHtml(mode);
        }
        if (INJECTION_PATTERN.matcher(question).find()) {
            return "Input rejected by content policy.";
        }
        return null;
    }

    private String renderAnswer(String question, String answer) {
        Node document = PARSER.parse(answer);
        String html = RENDERER.render(document);
        return """
                <div class="chat-message">
                    <div class="question">%s</div>
                    <div class="answer">%s</div>
                </div>
                """.formatted(escapeHtml(question), html);
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
