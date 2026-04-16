package com.azul.eclipseocx2026.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * CDI interceptor that produces structured audit log entries for agent interactions.
 *
 * Log format: AUDIT | key=value pairs
 *
 * Entries include: method, mode, truncated question, result length,
 * execution latency (ms), and status (success/error).
 *
 * The {@code audit.max-question-length} config property controls how much
 * of the question text is logged (default: 80 chars).
 */
@Audited
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditInterceptor {

    private static final Logger LOG = Logger.getLogger(AuditInterceptor.class.getName());

    @Inject
    @ConfigProperty(name = "audit.max-question-length", defaultValue = "80")
    int maxQuestionLength;

    @AroundInvoke
    public Object audit(InvocationContext ctx) throws Exception {
        String method = ctx.getMethod().getName();
        Object[] params = ctx.getParameters();
        long start = System.currentTimeMillis();

        try {
            Object result = ctx.proceed();
            long duration = System.currentTimeMillis() - start;
            int resultLength = result instanceof String s ? s.length() : -1;

            LOG.info(String.format(
                    "AUDIT | method=%s | params=%s | result_length=%d | duration=%dms | status=success",
                    method, truncateParams(params), resultLength, duration));
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            LOG.warning(String.format(
                    "AUDIT | method=%s | params=%s | duration=%dms | status=error | error=%s",
                    method, truncateParams(params), duration, e.getMessage()));
            throw e;
        }
    }

    private String truncateParams(Object[] params) {
        if (params == null || params.length == 0) return "[]";
        String raw = Arrays.stream(params)
                .map(p -> p instanceof String s && s.length() > maxQuestionLength
                        ? s.substring(0, maxQuestionLength) + "..."
                        : String.valueOf(p))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        return "[" + raw + "]";
    }
}
