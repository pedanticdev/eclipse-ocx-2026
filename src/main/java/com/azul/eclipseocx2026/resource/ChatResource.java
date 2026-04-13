package com.azul.eclipseocx2026.resource;

import com.azul.eclipseocx2026.ai.ConferenceChatService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/chat")
public class ChatResource {

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
        return """
                <div class="chat-message">
                    <div class="question">%s</div>
                    <div class="answer">%s</div>
                </div>
                """.formatted(escapeHtml(question), escapeHtml(answer));
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
