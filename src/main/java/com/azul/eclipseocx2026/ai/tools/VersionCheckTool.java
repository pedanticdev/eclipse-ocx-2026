package com.azul.eclipseocx2026.ai.tools;

import ai.koog.agents.core.tools.annotations.LLMDescription;
import ai.koog.agents.core.tools.annotations.Tool;
import ai.koog.agents.core.tools.reflect.ToolSet;

import java.util.Map;

/**
 * Koog ToolSet providing Jakarta EE version lookup.
 * Static data covering the major specifications and their version history.
 */
public class VersionCheckTool implements ToolSet {

    private static final Map<String, String> VERSION_DATA = Map.ofEntries(
            Map.entry("cdi", "CDI 4.1 in Jakarta EE 11 (Virtual Threads, build paths). CDI 4.0 in Jakarta EE 10. CDI 2.0 in Java EE 8."),
            Map.entry("jpa", "Jakarta Persistence 3.2 in Jakarta EE 11 (Java records as embeddables, UUID generation). JPA 3.1 in Jakarta EE 10."),
            Map.entry("jax-rs", "Jakarta RESTful Web Services 4.0 in Jakarta EE 11. JAX-RS 3.1 in Jakarta EE 10. JAX-RS 2.1 in Java EE 8."),
            Map.entry("servlet", "Jakarta Servlet 6.1 in Jakarta EE 11. Servlet 6.0 in Jakarta EE 10. Servlet 4.0 in Java EE 8."),
            Map.entry("concurrency", "Jakarta Concurrency 3.1 in Jakarta EE 11 (Virtual Threads support, @Asynchronous). Concurrency 2.0 in Jakarta EE 10."),
            Map.entry("data", "Jakarta Data 1.0 introduced in Jakarta EE 11. Repository abstraction over JPA and NoSQL."),
            Map.entry("security", "Jakarta Security 4.0 in Jakarta EE 11. OpenID Connect container-managed authentication."),
            Map.entry("faces", "Jakarta Faces 4.1 in Jakarta EE 11. Faces 4.0 in Jakarta EE 10."),
            Map.entry("messaging", "Jakarta Messaging 3.1 in Jakarta EE 11. Simplified API with CDI integration."),
            Map.entry("json", "Jakarta JSON Processing 2.1 and JSON Binding 3.0 in Jakarta EE 11."),
            Map.entry("validation", "Jakarta Validation 3.1 in Jakarta EE 11. Cross-parameter constraints."),
            Map.entry("websocket", "Jakarta WebSocket 2.2 in Jakarta EE 11."),
            Map.entry("batch", "Jakarta Batch 2.1 in Jakarta EE 11."),
            Map.entry("mail", "Jakarta Mail 2.1 in Jakarta EE 11."),
            Map.entry("annotations", "Jakarta Annotations 3.0 in Jakarta EE 11."),
            Map.entry("ejb", "Jakarta Enterprise Beans 4.0 in Jakarta EE 11. Simplified, aligned with CDI."),
            Map.entry("interceptors", "Jakarta Interceptors 2.2 in Jakarta EE 11."),
            Map.entry("web", "Jakarta EE 11 requires Java 21+. Key additions: Virtual Threads, Jakarta Data, improved Concurrency.")
    );

    @Tool
    @LLMDescription("Look up which Jakarta EE version introduced a specific feature or API")
    public String checkVersion(@LLMDescription("The Jakarta EE feature, API, or specification name (e.g. 'cdi', 'concurrency', 'data')") String feature) {
        String normalized = feature.toLowerCase().trim();

        String exact = VERSION_DATA.get(normalized);
        if (exact != null) {
            return exact;
        }

        for (Map.Entry<String, String> entry : VERSION_DATA.entrySet()) {
            if (normalized.contains(entry.getKey()) || entry.getKey().contains(normalized)) {
                return entry.getValue();
            }
        }

        return "Version information not found for: " + feature
                + ". Known specifications: " + String.join(", ", VERSION_DATA.keySet());
    }
}
