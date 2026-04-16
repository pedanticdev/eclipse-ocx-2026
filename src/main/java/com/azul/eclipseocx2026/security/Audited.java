package com.azul.eclipseocx2026.security;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for audit logging via CDI interceptor.
 *
 * The interceptor logs method name, truncated parameters, result size,
 * and execution latency as structured key=value pairs. This creates
 * a deterministic audit trail for all agent interactions.
 */
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
}
