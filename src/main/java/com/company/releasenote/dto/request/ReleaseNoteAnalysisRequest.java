package com.company.releasenote.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReleaseNoteAnalysisRequest(
        @NotBlank(message = "Release note boş bırakılamaz.")
        String releaseNote
) {
}
