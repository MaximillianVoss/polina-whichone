package com.whichone.dto;

import com.whichone.domain.QuizType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Request and response contracts are separate: JPA entities are never HTTP responses. */
public final class ApiDtos {
    private ApiDtos() { }

    public record AuthRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{3,30}") String username,
            @NotBlank @Size(min = 10, max = 72) String password) { }
    public record UserView(long id, String username, Instant createdAt) { }
    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserView user) { }

    public record QuizInput(
            @NotBlank @Size(max = 120) String title,
            @NotNull @Size(max = 1000) String description,
            @NotNull QuizType type,
            @NotNull @Size(min = 1, max = 30) List<@NotNull @Valid QuestionInput> questions,
            @NotNull @Size(max = 8) List<@NotNull @Valid OutcomeInput> outcomes) { }
    public record QuestionInput(
            @NotBlank @Size(max = 1000) String text,
            @NotNull @Min(0) @Max(10) Integer points,
            @NotNull @Size(min = 2, max = 8) List<@NotNull @Valid OptionInput> options) { }
    public record OptionInput(
            @NotBlank @Size(max = 300) String text,
            boolean correct,
            @Size(max = 24) String outcomeCode,
            @Min(0) @Max(10) int weight) { }
    public record OutcomeInput(
            @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,23}") String code,
            @NotBlank @Size(max = 120) String label,
            @NotNull @Size(max = 500) String description) { }

    public record QuizSummary(long id, String author, String title, String description, QuizType type,
                              boolean published, int questionCount, Instant createdAt, Instant publishedAt) { }
    public record QuizView(long id, String author, String title, String description, QuizType type,
                           boolean published, Instant createdAt, Instant publishedAt,
                           List<QuestionView> questions, List<OutcomeView> outcomes) { }
    public record QuestionView(long id, String text, int points, List<OptionView> options) { }
    // The correctness flag and personality weights exist only in OptionInput and in storage.
    public record OptionView(long id, String text) { }
    public record OutcomeView(String code, String label, String description) { }

    public record AttemptInput(
            @NotNull @Size(min = 1, max = 30) List<@NotNull @Valid AnswerInput> answers) { }
    public record AnswerInput(@NotNull @Positive Long questionId, @NotNull @Positive Long optionId) { }
    public record AnswerFeedback(long questionId, long optionId, Boolean correct) { }
    public record AttemptView(long id, long quizId, String quizTitle, QuizType type, Instant submittedAt,
                              Integer score, Integer maxScore, String outcomeCode, String outcomeLabel,
                              String outcomeDescription, List<AnswerFeedback> answers) { }
    public record StatisticsView(long totalAttempts, long educationalAttempts, long personalityAttempts,
                                 Double averagePercent, Double bestPercent, Map<String, Long> personalityResults) { }
}
