package com.whichone.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoringServiceTest {
    private final ScoringService scoring = new ScoringService();

    @Test
    void educationalUsesQuestionPointsAndNotNumberOfAnswers() {
        var result = scoring.educational(List.of(
                new ScoringService.EducationalAnswer(true, 3),
                new ScoringService.EducationalAnswer(false, 2),
                new ScoringService.EducationalAnswer(true, 1)));
        assertThat(result.score()).isEqualTo(4);
        assertThat(result.maxScore()).isEqualTo(6);
    }

    @Test
    void personalitySelectsHighestTotal() {
        var result = scoring.personality(List.of("PLANNER", "EXPLORER"), List.of(
                new ScoringService.PersonalityVote("PLANNER", 1),
                new ScoringService.PersonalityVote("EXPLORER", 3)));
        assertThat(result).isEqualTo("EXPLORER");
    }

    @Test
    void personalityTieUsesDeclaredOutcomeOrderRatherThanAlphabet() {
        var result = scoring.personality(List.of("ZEBRA", "ALPHA"), List.of(
                new ScoringService.PersonalityVote("ALPHA", 2),
                new ScoringService.PersonalityVote("ZEBRA", 2)));
        assertThat(result).isEqualTo("ZEBRA");
    }

    @Test
    void allZeroVotesStillHaveDeterministicResult() {
        assertThat(scoring.personality(List.of("FIRST", "SECOND"),
                List.of(new ScoringService.PersonalityVote("SECOND", 0)))).isEqualTo("FIRST");
    }

    @Test
    void unknownOutcomeIsRejectedInsteadOfIgnored() {
        assertThatThrownBy(() -> scoring.personality(List.of("FIRST", "SECOND"),
                List.of(new ScoringService.PersonalityVote("FOREIGN", 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
