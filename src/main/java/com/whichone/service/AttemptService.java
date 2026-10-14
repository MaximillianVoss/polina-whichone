package com.whichone.service;

import com.whichone.domain.*;
import com.whichone.dto.ApiDtos.*;
import com.whichone.exception.ApiException;
import com.whichone.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttemptService {
    private final AttemptRepository attempts;
    private final QuizRepository quizzes;
    private final AuthService auth;
    private final ScoringService scoring;

    public AttemptService(AttemptRepository attempts, QuizRepository quizzes, AuthService auth, ScoringService scoring) {
        this.attempts = attempts;
        this.quizzes = quizzes;
        this.auth = auth;
        this.scoring = scoring;
    }

    public AttemptView submit(long quizId, String username, AttemptInput input) {
        var user = auth.requireUser(username);
        var quiz = quizzes.findById(quizId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found."));
        if (!quiz.isPublished()) {
            if (!quiz.getAuthor().getUsername().equals(username)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Quiz not found.");
            }
            throw new ApiException(HttpStatus.CONFLICT, "Publish the quiz before submitting answers.");
        }
        var selected = validatedOptions(quiz, input);
        Integer score = null;
        Integer maxScore = null;
        String outcomeCode = null;
        if (quiz.getType() == QuizType.EDUCATIONAL) {
            var result = scoring.educational(selected.stream().map(option ->
                    new ScoringService.EducationalAnswer(option.isCorrect(), option.getQuestion().getPoints())).toList());
            score = result.score();
            maxScore = result.maxScore();
        } else {
            outcomeCode = scoring.personality(quiz.getOutcomes().stream().map(PersonalityOutcome::getCode).toList(),
                    selected.stream().map(option ->
                            new ScoringService.PersonalityVote(option.getOutcomeCode(), option.getWeight())).toList());
        }
        var attempt = new QuizAttempt(user, quiz, score, maxScore, outcomeCode);
        selected.forEach(attempt::addAnswer);
        // Validation, calculation and persistence are one transaction: an invalid answer saves nothing.
        return view(attempts.saveAndFlush(attempt));
    }

    @Transactional(readOnly = true)
    public List<AttemptView> history(String username) {
        auth.requireUser(username);
        return attempts.findByUserUsernameOrderBySubmittedAtDesc(username).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public AttemptView get(long id, String username) {
        auth.requireUser(username);
        return view(attempts.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found.")));
    }

    @Transactional(readOnly = true)
    public StatisticsView statistics(String username) {
        auth.requireUser(username);
        var all = attempts.findByUserUsernameOrderBySubmittedAtDesc(username);
        var educational = all.stream().filter(attempt -> attempt.getMaxScore() != null).toList();
        Double average = null;
        Double best = null;
        if (!educational.isEmpty()) {
            average = round(educational.stream().mapToDouble(this::percentage).average().orElseThrow());
            best = round(educational.stream().mapToDouble(this::percentage).max().orElseThrow());
        }
        // Keys include quiz ID so the same outcome code in two quizzes is not conflated.
        var personality = all.stream().filter(attempt -> attempt.getOutcomeCode() != null)
                .collect(Collectors.groupingBy(attempt -> attempt.getQuiz().getId() + ":" + attempt.getOutcomeCode(),
                        TreeMap::new, Collectors.counting()));
        return new StatisticsView(all.size(), educational.size(), all.size() - educational.size(),
                average, best, personality);
    }

    private double percentage(QuizAttempt attempt) { return 100.0 * attempt.getScore() / attempt.getMaxScore(); }
    private double round(double value) { return Math.round(value * 100.0) / 100.0; }

    private List<AnswerOption> validatedOptions(Quiz quiz, AttemptInput input) {
        if (input.answers().size() != quiz.getQuestions().size()) {
            throw invalidAnswers();
        }
        var byQuestion = new HashMap<Long, Long>();
        for (var answer : input.answers()) {
            if (byQuestion.putIfAbsent(answer.questionId(), answer.optionId()) != null) {
                throw invalidAnswers();
            }
        }
        var selected = new ArrayList<AnswerOption>();
        for (var question : quiz.getQuestions()) {
            var optionId = byQuestion.get(question.getId());
            if (optionId == null) { throw invalidAnswers(); }
            // Look up only inside this question: IDs from another question or quiz cannot pass.
            selected.add(question.getOptions().stream().filter(option -> option.getId().equals(optionId))
                    .findFirst().orElseThrow(this::invalidAnswers));
        }
        return selected;
    }

    private ApiException invalidAnswers() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Submit exactly one valid option for every question in this quiz.");
    }

    private AttemptView view(QuizAttempt attempt) {
        var quiz = attempt.getQuiz();
        var outcome = quiz.getOutcomes().stream()
                .filter(candidate -> candidate.getCode().equals(attempt.getOutcomeCode())).findFirst().orElse(null);
        var feedback = attempt.getAnswers().stream().map(AttemptAnswer::getOption)
                .sorted(Comparator.comparingInt(option -> option.getQuestion().getPosition()))
                .map(option -> new AnswerFeedback(option.getQuestion().getId(), option.getId(),
                        quiz.getType() == QuizType.EDUCATIONAL ? option.isCorrect() : null)).toList();
        return new AttemptView(attempt.getId(), quiz.getId(), quiz.getTitle(), quiz.getType(), attempt.getSubmittedAt(),
                attempt.getScore(), attempt.getMaxScore(), attempt.getOutcomeCode(),
                outcome == null ? null : outcome.getLabel(), outcome == null ? null : outcome.getDescription(), feedback);
    }
}
