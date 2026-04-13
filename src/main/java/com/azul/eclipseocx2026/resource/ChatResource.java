package com.azul.eclipseocx2026.resource;

import com.azul.eclipseocx2026.ai.ConferenceChatService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

@Path("/chat")
public class ChatResource {

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    @Inject
    private ConferenceChatService chatService;

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public String ask(@FormParam("question") String question) {
        String answer = chatService.ask(question);
        return renderAnswer(question, answer);
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
