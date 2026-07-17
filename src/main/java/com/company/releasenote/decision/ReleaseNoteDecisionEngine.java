package com.company.releasenote.decision;

import com.company.releasenote.model.AnalysisStatus;
import com.company.releasenote.model.Coverage;
import com.company.releasenote.model.Requirement;
import org.springframework.stereotype.Component;

@Component
public class ReleaseNoteDecisionEngine {

    public DecisionResult decide(
            Requirement requirement,
            boolean configurationBlank,
            Coverage coverage
    ) {
        if (requirement == null) {
            throw new IllegalArgumentException(
                    "Requirement boş olamaz."
            );
        }

        return switch (requirement) {
            case NOT_REQUIRED -> noConfigurationRequired();

            case UNCERTAIN -> uncertainRequirement();

            case REQUIRED -> decideRequiredConfiguration(
                    configurationBlank,
                    coverage
            );
        };
    }

    private DecisionResult decideRequiredConfiguration(
            boolean configurationBlank,
            Coverage coverage
    ) {
        if (configurationBlank) {
            return new DecisionResult(
                    AnalysisStatus.MISSING_CONFIGURATION,
                    true,
                    "Configuration gereklidir ancak ilgili bilgi bulunmamaktadır."
            );
        }

        if (coverage == null) {
            throw new IllegalArgumentException(
                    "Configuration doluysa coverage sonucu boş olamaz."
            );
        }

        return switch (coverage) {
            case SUFFICIENT -> new DecisionResult(
                    AnalysisStatus.COMPLETE,
                    true,
                    "Gerekli Configuration bilgisi yeterli görünmektedir."
            );

            case INCOMPLETE -> new DecisionResult(
                    AnalysisStatus.INCOMPLETE_CONFIGURATION,
                    true,
                    "Configuration bölümü ilgili ancak bilgi yetersizdir."
            );

            case UNRELATED -> new DecisionResult(
                    AnalysisStatus.MISSING_CONFIGURATION,
                    true,
                    "Configuration bölümü doludur ancak Description ile ilgili değildir."
            );

            case UNCERTAIN -> new DecisionResult(
                    AnalysisStatus.UNCERTAIN,
                    true,
                    "Configuration bilgisinin yeterliliği belirlenememiştir."
            );
        };
    }

    private DecisionResult noConfigurationRequired() {
        return new DecisionResult(
                AnalysisStatus.NO_CONFIGURATION_REQUIRED,
                false,
                "Description için ek Configuration gerekmemektedir."
        );
    }

    private DecisionResult uncertainRequirement() {
        return new DecisionResult(
                AnalysisStatus.UNCERTAIN,
                false,
                "Description için Configuration gerekip gerekmediği belirlenememiştir."
        );
    }
}