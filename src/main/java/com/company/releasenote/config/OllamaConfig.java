package com.company.releasenote.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class OllamaConfig {

    @Bean
    public RestClient ollamaRestClient(
            RestClient.Builder builder,
            OllamaProperties properties
    ){
        return builder
                .baseUrl(properties.baseUrl())
                .build();
    }
}
