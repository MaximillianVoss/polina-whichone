package com.whichone.controller;

import com.whichone.dto.ApiDtos.*;
import com.whichone.service.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@Tag(name = "Quizzes")
public class QuizController {
    private final QuizService quizzes;
    private final AttemptService attempts;
    public QuizController(QuizService quizzes, AttemptService attempts) {
        this.quizzes = quizzes;
        this.attempts = attempts;
    }

    @GetMapping
    @Operation(summary = "Get the published quiz feed")
    public List<QuizSummary> published() { return quizzes.published(); }

    @GetMapping("/mine")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get your own drafts and published quizzes")
    public List<QuizSummary> mine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return quizzes.mine(principal.getSubject());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a quiz without answer keys", description = "Drafts are visible only to their author; otherwise 404.")
    public QuizView get(@PathVariable long id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return quizzes.get(id, principal == null ? null : principal.getSubject());
    }

    @PostMapping
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a draft", description = "201; author is always the authenticated user. Semantic validation returns 400.")
    public ResponseEntity<QuizView> create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal,
                                          @Valid @RequestBody QuizInput input) {
        var quiz = quizzes.create(principal.getSubject(), input);
        return ResponseEntity.created(URI.create("/api/quizzes/" + quiz.id())).body(quiz);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Replace your draft definition", description = "200; 403 foreign quiz, 409 published quiz. Child IDs change after replacement.")
    public QuizView update(@PathVariable long id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt principal,
                           @Valid @RequestBody QuizInput input) {
        return quizzes.update(id, principal.getSubject(), input);
    }

    @PostMapping("/{id}/publish")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Publish your quiz permanently", description = "200; repeated publication is idempotent; foreign author gets 403.")
    public QuizView publish(@PathVariable long id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return quizzes.publish(id, principal.getSubject());
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete your draft", description = "204; 403 foreign quiz, 409 published quiz.")
    public ResponseEntity<Void> delete(@PathVariable long id,
                                      @Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        quizzes.delete(id, principal.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/attempts")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Submit one option per question and calculate the result",
            description = "201; 400 invalid/foreign option, 409 own unpublished quiz, 404 unavailable quiz. Client scores are never accepted.")
    public ResponseEntity<AttemptView> submit(@PathVariable long id,
                    @Parameter(hidden = true) @AuthenticationPrincipal Jwt principal,
                    @Valid @RequestBody AttemptInput input) {
        var result = attempts.submit(id, principal.getSubject(), input);
        return ResponseEntity.created(URI.create("/api/me/attempts/" + result.id())).body(result);
    }
}
