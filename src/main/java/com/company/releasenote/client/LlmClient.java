package com.company.releasenote.client;

import com.company.releasenote.dto.llm.CoverageAnalysis;
import com.company.releasenote.dto.llm.DescriptionAnalysis;
import com.company.releasenote.model.ChangeCategory;

public interface LlmClient {
    DescriptionAnalysis analyzeDescription(
            String description
    );
    CoverageAnalysis analyzeCoverage(
            ChangeCategory category,
            String description,
            String configuration);

}
