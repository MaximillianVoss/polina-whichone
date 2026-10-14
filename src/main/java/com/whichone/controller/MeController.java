package com.whichone.controller;

import com.whichone.dto.ApiDtos.*;
import com.whichone.service.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/me")
@Tag(name = "My account and results")
@SecurityRequirement(name = "BearerAuth")
public class MeController {
    private final AuthService auth;
    private final AttemptService attempts;
    public MeController(AuthService auth, AttemptService attempts) { this.auth = auth; this.attempts = attempts; }

    @GetMapping
    @Operation(summary = "Get your safe account profile")
    public UserView me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return auth.me(principal.getSubject());
    }

    @GetMapping("/attempts")
    @Operation(summary = "Get your attempt history")
    public List<AttemptView> history(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return attempts.history(principal.getSubject());
    }

    @GetMapping("/attempts/{id}")
    @Operation(summary = "Get one of your results", description = "A missing or foreign result returns 404.")
    public AttemptView result(@PathVariable long id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return attempts.get(id, principal.getSubject());
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get statistics calculated from your saved attempts",
            description = "Percentages apply only to educational attempts. Unknown averages are null, not zero.")
    public StatisticsView statistics(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return attempts.statistics(principal.getSubject());
    }
}
