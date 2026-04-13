package com.azul.eclipseocx2026.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;

@Path("/")
public class PageResource {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public InputStream index() {
        return getClass().getResourceAsStream("/WEB-INF/views/index.html");
    }
}
