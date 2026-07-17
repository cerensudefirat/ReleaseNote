package com.company.releasenote.dto.llm;

import com.company.releasenote.model.ChangeCategory;
import com.company.releasenote.model.Requirement;

public record DescriptionAnalysis(
        Requirement requirement,
        ChangeCategory category

) {
}
