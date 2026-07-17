package com.company.releasenote.dto.llm.ollama;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatMessage(
        String role,
        String content
) {
}
