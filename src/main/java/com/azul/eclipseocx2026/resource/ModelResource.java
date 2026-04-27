package com.azul.eclipseocx2026.resource;

import com.azul.eclipseocx2026.ai.OllamaChat;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML;

@Path("/models")
public class ModelResource {

    @Inject
    private OllamaChat ollamaChat;

    private static final System.Logger LOG = System.getLogger(ModelResource.class.getName());

    @GET
    @Produces(APPLICATION_JSON)
    public Map<String, Object> listModels() {
        LOG.log(System.Logger.Level.INFO, "GET /models - current: {0}", ollamaChat.getCurrentModel());
        return Map.of(
                "current", ollamaChat.getCurrentModel(),
                "available", ollamaChat.getAvailableModels()
        );
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_HTML)
    public String switchModel(@FormParam("model") String modelName) {
        LOG.log(System.Logger.Level.INFO, "POST /models - switching to: {0}", modelName);
        ollamaChat.switchModel(modelName);
        return """
                <div id="model-status" class="model-switched">
                    <span class="current-label">Active:</span> <strong>%s</strong>
                </div>""".formatted(modelName);
    }
}
