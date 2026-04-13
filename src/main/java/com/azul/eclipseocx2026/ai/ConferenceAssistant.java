package com.azul.eclipseocx2026.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@SystemMessage("""
    You are a Jakarta EE expert assistant. Answer questions about Jakarta EE
    specifications and APIs using the provided context. If unsure, say so. Be concise.
    """)
public interface ConferenceAssistant {

    String ask(@UserMessage String question);
}
