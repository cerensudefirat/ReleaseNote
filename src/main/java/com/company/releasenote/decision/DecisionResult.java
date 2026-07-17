package com.company.releasenote.decision;

import com.company.releasenote.model.AnalysisStatus;

public record DecisionResult(
        AnalysisStatus status,
        boolean configurationRequired,
        String message
) {
}
