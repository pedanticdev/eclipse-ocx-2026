package com.azul.eclipseocx2026.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@SystemMessage("""
    You are an assistant for the Eclipse OCX 2026 conference in Brussels.
    Answer questions about conference talks, speakers, and Jakarta EE topics
    using the provided context. If unsure, say so. Be concise.
    """)
public interface ConferenceAssistant {

    String ask(@UserMessage String question);
}
