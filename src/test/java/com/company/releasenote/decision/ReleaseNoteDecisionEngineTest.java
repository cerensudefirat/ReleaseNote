package com.company.releasenote.decision;

import com.company.releasenote.model.AnalysisStatus;
import com.company.releasenote.model.Coverage;
import com.company.releasenote.model.Requirement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseNoteDecisionEngineTest {

    private final ReleaseNoteDecisionEngine decisionEngine =
            new ReleaseNoteDecisionEngine();

    @Test
    void shouldReturnMissingWhenConfigurationIsRequiredButBlank() {
        DecisionResult result = decisionEngine.decide(
                Requirement.REQUIRED,
                true,
                null
        );

        assertEquals(
                AnalysisStatus.MISSING_CONFIGURATION,
                result.status()
        );

        assertTrue(result.configurationRequired());
    }

    @Test
    void shouldReturnCompleteWhenCoverageIsSufficient() {
        DecisionResult result = decisionEngine.decide(
                Requirement.REQUIRED,
                false,
                Coverage.SUFFICIENT
        );

        assertEquals(
                AnalysisStatus.COMPLETE,
                result.status()
        );

        assertTrue(result.configurationRequired());
    }

    @Test
    void shouldReturnIncompleteWhenCoverageIsIncomplete() {
        DecisionResult result = decisionEngine.decide(
                Requirement.REQUIRED,
                false,
                Coverage.INCOMPLETE
        );

        assertEquals(
                AnalysisStatus.INCOMPLETE_CONFIGURATION,
                result.status()
        );

        assertTrue(result.configurationRequired());
    }

    @Test
    void shouldReturnMissingWhenCoverageIsUnrelated() {
        DecisionResult result = decisionEngine.decide(
                Requirement.REQUIRED,
                false,
                Coverage.UNRELATED
        );

        assertEquals(
                AnalysisStatus.MISSING_CONFIGURATION,
                result.status()
        );

        assertTrue(result.configurationRequired());
    }

    @Test
    void shouldReturnNoConfigurationRequired() {
        DecisionResult result = decisionEngine.decide(
                Requirement.NOT_REQUIRED,
                true,
                null
        );

        assertEquals(
                AnalysisStatus.NO_CONFIGURATION_REQUIRED,
                result.status()
        );

        assertFalse(result.configurationRequired());
    }

    @Test
    void shouldReturnUncertainWhenRequirementIsUncertain() {
        DecisionResult result = decisionEngine.decide(
                Requirement.UNCERTAIN,
                true,
                null
        );

        assertEquals(
                AnalysisStatus.UNCERTAIN,
                result.status()
        );

        assertFalse(result.configurationRequired());
    }

    @Test
    void shouldThrowExceptionWhenCoverageIsMissing() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> decisionEngine.decide(
                                Requirement.REQUIRED,
                                false,
                                null
                        )
                );

        assertEquals(
                "Configuration doluysa coverage sonucu boş olamaz.",
                exception.getMessage()
        );
    }
    @ParameterizedTest
    @MethodSource("decisionScenarios")
    void shouldReturnExpectedStatusForDifferentScenarios(
            Requirement requirement,
            boolean configurationBlank,
            Coverage coverage,
            AnalysisStatus expectedStatus,
            boolean expectedConfigurationRequired
    ) {
        DecisionResult result = decisionEngine.decide(
                requirement,
                configurationBlank,
                coverage
        );

        assertEquals(
                expectedStatus,
                result.status()
        );

        assertEquals(
                expectedConfigurationRequired,
                result.configurationRequired()
        );
    }
    private static Stream<Arguments> decisionScenarios() {
        return Stream.of(
                Arguments.of(
                        Requirement.REQUIRED,
                        true,
                        null,
                        AnalysisStatus.MISSING_CONFIGURATION,
                        true
                ),
                Arguments.of(
                        Requirement.REQUIRED,
                        false,
                        Coverage.SUFFICIENT,
                        AnalysisStatus.COMPLETE,
                        true
                ),
                Arguments.of(
                        Requirement.REQUIRED,
                        false,
                        Coverage.INCOMPLETE,
                        AnalysisStatus.INCOMPLETE_CONFIGURATION,
                        true
                ),
                Arguments.of(
                        Requirement.REQUIRED,
                        false,
                        Coverage.UNRELATED,
                        AnalysisStatus.MISSING_CONFIGURATION,
                        true
                ),
                Arguments.of(
                        Requirement.REQUIRED,
                        false,
                        Coverage.UNCERTAIN,
                        AnalysisStatus.UNCERTAIN,
                        true
                ),
                Arguments.of(
                        Requirement.NOT_REQUIRED,
                        true,
                        null,
                        AnalysisStatus.NO_CONFIGURATION_REQUIRED,
                        false
                ),
                Arguments.of(
                        Requirement.NOT_REQUIRED,
                        false,
                        Coverage.SUFFICIENT,
                        AnalysisStatus.NO_CONFIGURATION_REQUIRED,
                        false
                ),
                Arguments.of(
                        Requirement.UNCERTAIN,
                        true,
                        null,
                        AnalysisStatus.UNCERTAIN,
                        false
                )
        );
    }
}