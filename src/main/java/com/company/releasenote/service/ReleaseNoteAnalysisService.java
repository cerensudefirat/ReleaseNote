package com.company.releasenote.service;

import com.company.releasenote.client.LlmClient;
import com.company.releasenote.decision.DecisionResult;
import com.company.releasenote.decision.ReleaseNoteDecisionEngine;
import com.company.releasenote.dto.llm.CoverageAnalysis;
import com.company.releasenote.dto.llm.DescriptionAnalysis;
import com.company.releasenote.dto.response.ReleaseNoteAnalysisResponse;
import com.company.releasenote.exception.InvalidLlmResponseException;
import com.company.releasenote.exception.InvalidReleaseNoteException;
import com.company.releasenote.model.Coverage;
import com.company.releasenote.model.Requirement;
import com.company.releasenote.parser.ParsedReleaseNote;
import com.company.releasenote.parser.ReleaseNoteParser;
import org.springframework.stereotype.Service;

@Service
public class ReleaseNoteAnalysisService {

    private final ReleaseNoteParser releaseNoteParser;
    private final LlmClient llmClient;
    private final ReleaseNoteDecisionEngine decisionEngine;

    public ReleaseNoteAnalysisService(
            ReleaseNoteParser releaseNoteParser,
            LlmClient llmClient,
            ReleaseNoteDecisionEngine decisionEngine
    ) {
        this.releaseNoteParser = releaseNoteParser;
        this.llmClient = llmClient;
        this.decisionEngine = decisionEngine;
    }

    public ReleaseNoteAnalysisResponse analyze(
            String releaseNote
    ) {
        ParsedReleaseNote parsedReleaseNote =
                releaseNoteParser.parse(releaseNote);

        validateParsedReleaseNote(parsedReleaseNote);

        DescriptionAnalysis descriptionAnalysis =
                llmClient.analyzeDescription(
                        parsedReleaseNote.description()
                );

        Coverage coverage = determineCoverage(
                parsedReleaseNote,
                descriptionAnalysis
        );

        DecisionResult decisionResult =
                decisionEngine.decide(
                        descriptionAnalysis.requirement(),
                        parsedReleaseNote.isConfigurationBlank(),
                        coverage
                );

        return new ReleaseNoteAnalysisResponse(
                decisionResult.status(),
                descriptionAnalysis.category(),
                decisionResult.configurationRequired(),
                decisionResult.message()
        );
    }

    private Coverage determineCoverage(
            ParsedReleaseNote parsedReleaseNote,
            DescriptionAnalysis descriptionAnalysis
    ) {
        if (descriptionAnalysis.requirement()
                != Requirement.REQUIRED) {
            return null;
        }

        if (parsedReleaseNote.isConfigurationBlank()) {
            return null;
        }

        CoverageAnalysis coverageAnalysis =
                llmClient.analyzeCoverage(
                        descriptionAnalysis.category(),
                        parsedReleaseNote.description(),
                        parsedReleaseNote.configuration()
                );

        return coverageAnalysis.coverage();
    }

    private void validateParsedReleaseNote(
            ParsedReleaseNote parsedReleaseNote
    ) {
        if (!parsedReleaseNote.descriptionSectionPresent()) {
            throw new InvalidReleaseNoteException(
                    "Description bölümü bulunamadı."
            );
        }

        if (parsedReleaseNote.isDescriptionBlank()) {
            throw new InvalidReleaseNoteException(
                    "Description bölümü boş bırakılamaz."
            );
        }
    }
}