package com.company.releasenote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ollama")
public record OllamaProperties(
        String baseUrl,
        String model,
        double temperature,
        int maxOutputTokens

) {
}
