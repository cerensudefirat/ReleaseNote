package com.company.releasenote.service;

import com.company.releasenote.client.LlmClient;
import com.company.releasenote.decision.ReleaseNoteDecisionEngine;
import com.company.releasenote.dto.llm.CoverageAnalysis;
import com.company.releasenote.dto.llm.DescriptionAnalysis;
import com.company.releasenote.dto.response.ReleaseNoteAnalysisResponse;
import com.company.releasenote.model.AnalysisStatus;
import com.company.releasenote.model.ChangeCategory;
import com.company.releasenote.model.Coverage;
import com.company.releasenote.model.Requirement;
import com.company.releasenote.parser.ReleaseNoteParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseNoteAnalysisServiceTest {

    @Mock
    private LlmClient llmClient;

    private ReleaseNoteAnalysisService analysisService;

    @BeforeEach
    void setUp() {
        ReleaseNoteParser parser =
                new ReleaseNoteParser();

        ReleaseNoteDecisionEngine decisionEngine =
                new ReleaseNoteDecisionEngine();

        analysisService = new ReleaseNoteAnalysisService(
                parser,
                llmClient,
                decisionEngine
        );
    }

    @Test
    void shouldReturnMissingWhenConfigurationIsBlank() {
        String releaseNote = """
                Description:
                Product tablosuna stock_status kolonu eklenmiştir.

                Configuration:
                """;

        when(llmClient.analyzeDescription(anyString()))
                .thenReturn(
                        new DescriptionAnalysis(
                                Requirement.REQUIRED,
                                ChangeCategory.DATABASE
                        )
                );

        ReleaseNoteAnalysisResponse result =
                analysisService.analyze(releaseNote);

        assertEquals(
                AnalysisStatus.MISSING_CONFIGURATION,
                result.status()
        );

        assertEquals(
                ChangeCategory.DATABASE,
                result.category()
        );

        assertTrue(result.configurationRequired());

        verify(
                llmClient,
                never()
        ).analyzeCoverage(
                any(ChangeCategory.class),
                anyString(),
                anyString()
        );
    }

    @Test
    void shouldReturnCompleteWhenConfigurationIsSufficient() {
        String releaseNote = """
                Description:
                Product tablosuna stock_status kolonu eklenmiştir.

                Configuration:
                ALTER TABLE Product ADD stock_status VARCHAR(20);
                """;

        when(llmClient.analyzeDescription(anyString()))
                .thenReturn(
                        new DescriptionAnalysis(
                                Requirement.REQUIRED,
                                ChangeCategory.DATABASE
                        )
                );

        when(llmClient.analyzeCoverage(
                eq(ChangeCategory.DATABASE),
                anyString(),
                anyString()
        )).thenReturn(
                new CoverageAnalysis(
                        Coverage.SUFFICIENT
                )
        );

        ReleaseNoteAnalysisResponse result =
                analysisService.analyze(releaseNote);

        assertEquals(
                AnalysisStatus.COMPLETE,
                result.status()
        );

        assertEquals(
                ChangeCategory.DATABASE,
                result.category()
        );

        assertTrue(result.configurationRequired());

        verify(llmClient).analyzeCoverage(
                eq(ChangeCategory.DATABASE),
                anyString(),
                anyString()
        );
    }

    @Test
    void shouldReturnIncompleteWhenConfigurationIsInsufficient() {
        String releaseNote = """
                Description:
                Product tablosuna stock_status kolonu eklenmiştir.

                Configuration:
                Required database changes must be applied.
                """;

        when(llmClient.analyzeDescription(anyString()))
                .thenReturn(
                        new DescriptionAnalysis(
                                Requirement.REQUIRED,
                                ChangeCategory.DATABASE
                        )
                );

        when(llmClient.analyzeCoverage(
                eq(ChangeCategory.DATABASE),
                anyString(),
                anyString()
        )).thenReturn(
                new CoverageAnalysis(
                        Coverage.INCOMPLETE
                )
        );

        ReleaseNoteAnalysisResponse result =
                analysisService.analyze(releaseNote);

        assertEquals(
                AnalysisStatus.INCOMPLETE_CONFIGURATION,
                result.status()
        );

        assertTrue(result.configurationRequired());
    }

    @Test
    void shouldSkipCoverageWhenConfigurationIsNotRequired() {
        String releaseNote = """
                Description:
                Kullanıcıya gösterilen hata mesajı değiştirilmiştir.

                Configuration:
                """;

        when(llmClient.analyzeDescription(anyString()))
                .thenReturn(
                        new DescriptionAnalysis(
                                Requirement.NOT_REQUIRED,
                                ChangeCategory.UI_CHANGE
                        )
                );

        ReleaseNoteAnalysisResponse result =
                analysisService.analyze(releaseNote);

        assertEquals(
                AnalysisStatus.NO_CONFIGURATION_REQUIRED,
                result.status()
        );

        assertEquals(
                ChangeCategory.UI_CHANGE,
                result.category()
        );

        assertFalse(result.configurationRequired());

        verify(
                llmClient,
                never()
        ).analyzeCoverage(
                any(ChangeCategory.class),
                anyString(),
                anyString()
        );
    }

    @Test
    void shouldReturnMissingWhenConfigurationIsUnrelated() {
        String releaseNote = """
                Description:
                Product tablosuna stock_status kolonu eklenmiştir.

                Configuration:
                Application port değeri 8081 yapılmalıdır.
                """;

        when(llmClient.analyzeDescription(anyString()))
                .thenReturn(
                        new DescriptionAnalysis(
                                Requirement.REQUIRED,
                                ChangeCategory.DATABASE
                        )
                );

        when(llmClient.analyzeCoverage(
                eq(ChangeCategory.DATABASE),
                anyString(),
                anyString()
        )).thenReturn(
                new CoverageAnalysis(
                        Coverage.UNRELATED
                )
        );

        ReleaseNoteAnalysisResponse result =
                analysisService.analyze(releaseNote);

        assertEquals(
                AnalysisStatus.MISSING_CONFIGURATION,
                result.status()
        );

        assertTrue(result.configurationRequired());
    }
}