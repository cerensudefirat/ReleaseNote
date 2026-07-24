package com.company.releasenote.controller;

import com.company.releasenote.dto.request.ReleaseNoteAnalysisRequest;
import com.company.releasenote.dto.response.ReleaseNoteAnalysisResponse;
import com.company.releasenote.service.ReleaseNoteAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/release-notes")
public class ReleaseNoteAnalysisController {

    private final ReleaseNoteAnalysisService analysisService;

    public ReleaseNoteAnalysisController(
            ReleaseNoteAnalysisService analysisService
    ) {
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    public ReleaseNoteAnalysisResponse analyze(@Valid @RequestBody ReleaseNoteAnalysisRequest request) {
        return analysisService.analyze(
                request.releaseNote().toString()
        );
    }
}