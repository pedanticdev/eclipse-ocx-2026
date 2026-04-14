package com.azul.eclipseocx2026.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;

@ApplicationScoped
@ManagedExecutorDefinition(
        name = "java:module/concurrent/VirtualThreadExecutor",
        virtual = true,
        qualifiers = VirtualThreadExecutor.class
)
public class ConcurrencyConfig {
}
