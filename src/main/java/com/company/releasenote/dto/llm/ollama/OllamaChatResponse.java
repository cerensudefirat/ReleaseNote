package com.company.releasenote.dto.llm.ollama;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatResponse(
        OllamaChatMessage message,
        boolean done,

        @JsonProperty("done_reason")
        String doneReason
) {
}
