package com.company.releasenote.dto.response;

import com.company.releasenote.model.AnalysisStatus;
import com.company.releasenote.model.ChangeCategory;

public record ReleaseNoteAnalysisResponse(
        AnalysisStatus status,
        ChangeCategory category,
        boolean configurationRequired,
        String message
) {
}
