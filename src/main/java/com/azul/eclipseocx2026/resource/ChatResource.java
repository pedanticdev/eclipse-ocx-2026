package com.azul.eclipseocx2026.resource;

import com.azul.eclipseocx2026.ai.AiService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML;

@Path("/chat")
public class ChatResource {

    @Inject
    private AiService aiService;

    private static final System.Logger LOG = System.getLogger(ChatResource.class.getName());
    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_HTML)
    public String ask(@FormParam("question") String question) {
        LOG.log(System.Logger.Level.INFO, "POST /chat - question: {0}", question);
        String answer = aiService.ask(question);
        return renderAnswer(question, answer);
    }

    private String renderAnswer(String question, String answer) {
        Node document = PARSER.parse(answer);
        String html = RENDERER.render(document);
        return """
                <div class="chat-message">
                    <div class="question">%s</div>
                    <div class="answer">%s</div>
                </div>""".formatted(escapeHtml(question), html);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
