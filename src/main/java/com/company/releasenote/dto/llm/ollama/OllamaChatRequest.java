package com.company.releasenote.dto.llm.ollama;

import java.util.List;
import java.util.Map;

public record OllamaChatRequest(
        String model,
        List<OllamaChatMessage> messages,
        boolean stream,
        boolean think,
        Map<String, Object> format,
        Map<String, Object> options
) {
    public OllamaChatRequest{
        if(messages==null || messages.isEmpty()){
            throw new IllegalArgumentException("Ollama mesaj listesi boş bulunamaz.");
        }
    }
}
