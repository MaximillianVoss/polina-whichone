package com.whichone.service;

import com.whichone.domain.*;
import com.whichone.dto.ApiDtos.*;
import com.whichone.exception.ApiException;
import com.whichone.repository.QuizRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class QuizService {
    private final QuizRepository quizzes;
    private final AuthService auth;
    public QuizService(QuizRepository quizzes, AuthService auth) {
        this.quizzes = quizzes;
        this.auth = auth;
    }

    public QuizView create(String username, QuizInput input) {
        validateDefinition(input);
        var quiz = new Quiz(auth.requireUser(username));
        fill(quiz, input);
        return view(quizzes.saveAndFlush(quiz));
    }

    public QuizView update(long id, String username, QuizInput input) {
        var quiz = ownedLocked(id, username);
        requireDraft(quiz);
        validateDefinition(input);
        quiz.clearDraftContents();
        // Delete old ordered children before inserting new ones with the same positions.
        quizzes.flush();
        fill(quiz, input);
        return view(quizzes.saveAndFlush(quiz));
    }

    public QuizView publish(long id, String username) {
        var quiz = ownedLocked(id, username);
        quiz.publish();
        return view(quizzes.saveAndFlush(quiz));
    }

    public void delete(long id, String username) {
        var quiz = ownedLocked(id, username);
        requireDraft(quiz);
        quizzes.delete(quiz);
        quizzes.flush();
    }

    @Transactional(readOnly = true)
    public List<QuizSummary> published() {
        return quizzes.findByPublishedTrueOrderByPublishedAtDesc().stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public List<QuizSummary> mine(String username) {
        auth.requireUser(username);
        return quizzes.findByAuthorUsernameOrderByCreatedAtDesc(username).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public QuizView get(long id, String username) {
        var quiz = quizzes.findById(id).orElseThrow(() -> notFound());
        if (!quiz.isPublished() && !quiz.getAuthor().getUsername().equals(username)) {
            // A stranger cannot enumerate unpublished quizzes.
            throw notFound();
        }
        return view(quiz);
    }

    private Quiz ownedLocked(long id, String username) {
        auth.requireUser(username);
        var quiz = quizzes.findLockedById(id).orElseThrow(() -> notFound());
        if (!quiz.getAuthor().getUsername().equals(username)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the author may change this quiz.");
        }
        return quiz;
    }

    private void requireDraft(Quiz quiz) {
        if (quiz.isPublished()) {
            throw new ApiException(HttpStatus.CONFLICT, "Published quizzes are immutable; create a new quiz.");
        }
    }

    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "Quiz not found."); }
    private void invalid(String message) { throw new ApiException(HttpStatus.BAD_REQUEST, message); }

    private void validateDefinition(QuizInput input) {
        var codes = new HashSet<String>();
        input.outcomes().forEach(outcome -> {
            if (!codes.add(outcome.code())) { invalid("Outcome codes must be unique."); }
        });
        if (input.type() == QuizType.EDUCATIONAL && !codes.isEmpty()) {
            invalid("Educational quizzes must have an empty outcomes list.");
        }
        if (input.type() == QuizType.PERSONALITY && codes.size() < 2) {
            invalid("Personality quizzes require at least two outcomes.");
        }
        for (var question : input.questions()) {
            var texts = new HashSet<String>();
            for (var option : question.options()) {
                if (!texts.add(option.text().trim().toLowerCase(Locale.ROOT))) {
                    invalid("Options in one question must have distinct text.");
                }
            }
            if (input.type() == QuizType.EDUCATIONAL) {
                if (question.points() < 1) { invalid("Educational questions require 1 to 10 points."); }
                long correctCount = question.options().stream().filter(OptionInput::correct).count();
                if (correctCount != 1) { invalid("Each educational question must have exactly one correct option."); }
                if (question.options().stream().anyMatch(option -> option.outcomeCode() != null || option.weight() != 0)) {
                    invalid("Educational options must not contain personality outcome codes or weights.");
                }
            } else {
                if (question.points() != 0) { invalid("Personality question points must be zero."); }
                if (question.options().stream().anyMatch(option -> option.correct() || !codes.contains(option.outcomeCode()))) {
                    invalid("Personality options need a declared outcome code and must not be marked correct.");
                }
            }
        }
    }

    private void fill(Quiz quiz, QuizInput input) {
        quiz.define(input.title().trim(), input.description().trim(), input.type());
        for (int i = 0; i < input.outcomes().size(); i++) {
            var outcome = input.outcomes().get(i);
            quiz.addOutcome(new PersonalityOutcome(quiz, outcome.code(), outcome.label().trim(),
                    outcome.description().trim(), i));
        }
        for (int i = 0; i < input.questions().size(); i++) {
            var definition = input.questions().get(i);
            var question = new Question(quiz, definition.text().trim(), i, definition.points());
            for (int j = 0; j < definition.options().size(); j++) {
                var option = definition.options().get(j);
                question.addOption(new AnswerOption(question, option.text().trim(), j, option.correct(),
                        option.outcomeCode(), option.weight()));
            }
            quiz.addQuestion(question);
        }
    }

    private QuizSummary summary(Quiz quiz) {
        return new QuizSummary(quiz.getId(), quiz.getAuthor().getUsername(), quiz.getTitle(), quiz.getDescription(),
                quiz.getType(), quiz.isPublished(), quiz.getQuestions().size(), quiz.getCreatedAt(), quiz.getPublishedAt());
    }

    private QuizView view(Quiz quiz) {
        var questions = quiz.getQuestions().stream().map(question ->
                new QuestionView(question.getId(), question.getText(), question.getPoints(),
                        question.getOptions().stream().map(option -> new OptionView(option.getId(), option.getText())).toList()))
                .toList();
        var outcomes = quiz.getOutcomes().stream().map(outcome ->
                new OutcomeView(outcome.getCode(), outcome.getLabel(), outcome.getDescription())).toList();
        return new QuizView(quiz.getId(), quiz.getAuthor().getUsername(), quiz.getTitle(), quiz.getDescription(),
                quiz.getType(), quiz.isPublished(), quiz.getCreatedAt(), quiz.getPublishedAt(), questions, outcomes);
    }
}
