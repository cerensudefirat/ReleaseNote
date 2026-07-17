package com.company.releasenote.client;

import com.company.releasenote.config.OllamaProperties;
import com.company.releasenote.dto.llm.CoverageAnalysis;
import com.company.releasenote.dto.llm.DescriptionAnalysis;
import com.company.releasenote.model.ChangeCategory;
import com.company.releasenote.model.Coverage;
import com.company.releasenote.model.Requirement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaLlmClientTest {

    private MockRestServiceServer mockServer;
    private OllamaLlmClient ollamaLlmClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder =
                RestClient.builder();

        mockServer = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        RestClient restClient = restClientBuilder
                .baseUrl("http://localhost:11434")
                .build();

        OllamaProperties properties =
                new OllamaProperties(
                        "http://localhost:11434",
                        "qwen3:4b-instruct",
                        0.0,
                        200
                );

        ollamaLlmClient = new OllamaLlmClient(
                restClient,
                new ObjectMapper(),
                properties
        );
    }

    @AfterEach
    void verifyRequests() {
        mockServer.verify();
    }

    @Test
    void shouldAnalyzeDescription() {
        String ollamaResponse = """
                {
                  "model": "qwen3:4b-instruct",
                  "message": {
                    "role": "assistant",
                    "content": "{\\"requirement\\":\\"REQUIRED\\",\\"category\\":\\"DATABASE\\"}"
                  },
                  "done": true,
                  "done_reason": "stop"
                }
                """;

        mockServer.expect(
                        once(),
                        requestTo(
                                "http://localhost:11434/api/chat"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "\"model\":\"qwen3:4b-instruct\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString("\"messages\"")
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Product tablosuna status kolonu eklenmiştir."
                                )
                        )
                )
                .andRespond(
                        withSuccess(
                                ollamaResponse,
                                MediaType.APPLICATION_JSON
                        )
                );

        DescriptionAnalysis result =
                ollamaLlmClient.analyzeDescription(
                        "Product tablosuna status kolonu eklenmiştir."
                );

        assertEquals(
                Requirement.REQUIRED,
                result.requirement()
        );

        assertEquals(
                ChangeCategory.DATABASE,
                result.category()
        );
    }

    @Test
    void shouldAnalyzeConfigurationCoverage() {
        String ollamaResponse = """
                {
                  "model": "qwen3:4b-instruct",
                  "message": {
                    "role": "assistant",
                    "content": "{\\"coverage\\":\\"INCOMPLETE\\"}"
                  },
                  "done": true,
                  "done_reason": "stop"
                }
                """;

        mockServer.expect(
                        once(),
                        requestTo(
                                "http://localhost:11434/api/chat"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().string(
                                containsString("\"messages\"")
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "\"coverage\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Required database changes must be applied."
                                )
                        )
                )
                .andRespond(
                        withSuccess(
                                ollamaResponse,
                                MediaType.APPLICATION_JSON
                        )
                );

        CoverageAnalysis result =
                ollamaLlmClient.analyzeCoverage(
                        ChangeCategory.DATABASE,
                        "Product tablosuna status kolonu eklenmiştir.",
                        "Required database changes must be applied."
                );

        assertEquals(
                Coverage.INCOMPLETE,
                result.coverage()
        );
    }
}