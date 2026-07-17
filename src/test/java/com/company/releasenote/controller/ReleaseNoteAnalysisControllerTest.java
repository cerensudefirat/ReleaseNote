package com.company.releasenote.controller;

import com.company.releasenote.dto.response.ReleaseNoteAnalysisResponse;
import com.company.releasenote.exception.InvalidLlmResponseException;
import com.company.releasenote.exception.InvalidReleaseNoteException;
import com.company.releasenote.exception.LlmServiceException;
import com.company.releasenote.model.AnalysisStatus;
import com.company.releasenote.model.ChangeCategory;
import com.company.releasenote.service.ReleaseNoteAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReleaseNoteAnalysisController.class)
class ReleaseNoteAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReleaseNoteAnalysisService analysisService;

    @Test
    void shouldReturnAnalysisResponse() throws Exception {
        ReleaseNoteAnalysisResponse serviceResponse =
                new ReleaseNoteAnalysisResponse(
                        AnalysisStatus.MISSING_CONFIGURATION,
                        ChangeCategory.DATABASE,
                        true,
                        "Configuration gereklidir ancak ilgili bilgi bulunmamaktadır."
                );

        when(analysisService.analyze(anyString()))
                .thenReturn(serviceResponse);

        String requestBody = """
                {
                  "releaseNote": "Description:\\nProduct tablosuna stock_status kolonu eklenmiştir.\\n\\nConfiguration:"
                }
                """;

        mockMvc.perform(
                        post("/api/release-notes/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("MISSING_CONFIGURATION")
                )
                .andExpect(
                        jsonPath("$.category")
                                .value("DATABASE")
                )
                .andExpect(
                        jsonPath("$.configurationRequired")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Configuration gereklidir ancak ilgili bilgi bulunmamaktadır."
                                )
                );

        verify(analysisService).analyze(
                "Description:\n"
                        + "Product tablosuna stock_status kolonu eklenmiştir."
                        + "\n\nConfiguration:"
        );
    }

    @Test
    void shouldReturnBadRequestWhenReleaseNoteIsBlank()
            throws Exception {

        String requestBody = """
                {
                  "releaseNote": "   "
                }
                """;

        mockMvc.perform(
                        post("/api/release-notes/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Release note boş bırakılamaz.")
                )
                .andExpect(
                        jsonPath("$.timestamp")
                                .exists()
                );

        verify(
                analysisService,
                never()
        ).analyze(anyString());
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionIsMissing()
            throws Exception {

        when(analysisService.analyze(anyString()))
                .thenThrow(
                        new InvalidReleaseNoteException(
                                "Description bölümü bulunamadı."
                        )
                );

        String requestBody = """
                {
                  "releaseNote": "Configuration:\\nDatabase changes are required."
                }
                """;

        mockMvc.perform(
                        post("/api/release-notes/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_RELEASE_NOTE")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Description bölümü bulunamadı.")
                )
                .andExpect(
                        jsonPath("$.timestamp")
                                .exists()
                );
    }

    @Test
    void shouldReturnBadRequestWhenJsonIsInvalid()
            throws Exception {

        String invalidJson = """
                {
                  "releaseNote": "Description: Test"
                """;

        mockMvc.perform(
                        post("/api/release-notes/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_JSON")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Gönderilen JSON yapısı okunamadı."
                                )
                );

        verify(
                analysisService,
                never()
        ).analyze(anyString());
    }
    @Test
    void shouldReturnBadGatewayWhenLlmResponseIsInvalid()
            throws Exception {

        when(analysisService.analyze(anyString()))
                .thenThrow(
                        new InvalidLlmResponseException(
                                "Ollama cevap içeriği boş."
                        )
                );

        String requestBody = """
            {
              "releaseNote": "Description:\\nProduct tablosuna status kolonu eklenmiştir.\\n\\nConfiguration:"
            }
            """;

        mockMvc.perform(
                        post("/api/release-notes/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadGateway())
                .andExpect(
                        jsonPath("$.status")
                                .value(502)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_LLM_RESPONSE")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Yapay zekâ servisinden geçerli bir analiz sonucu alınamadı."
                                )
                )
                .andExpect(
                        jsonPath("$.timestamp")
                                .exists()
                );
    }
    @Test
    void shouldReturnServiceUnavailableWhenLlmCannotBeReached()
            throws Exception {

        when(analysisService.analyze(anyString()))
                .thenThrow(
                        new LlmServiceException(
                                "Ollama servisine ulaşılamadı.",
                                new RuntimeException("Connection refused")
                        )
                );

        String requestBody = """
            {
              "releaseNote": "Description:\\nProduct tablosuna status kolonu eklenmiştir.\\n\\nConfiguration:"
            }
            """;

        mockMvc.perform(
                        post("/api/release-notes/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(
                        jsonPath("$.status")
                                .value(503)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("LLM_SERVICE_UNAVAILABLE")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Yapay zekâ servisine şu anda ulaşılamıyor."
                                )
                )
                .andExpect(
                        jsonPath("$.timestamp")
                                .exists()
                );
    }
}