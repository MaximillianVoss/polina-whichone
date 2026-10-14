package com.whichone.controller;

import com.whichone.dto.ApiDtos.*;
import com.whichone.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    @Operation(summary = "Register an ordinary user and issue a token", description = "201 on success, 400 invalid input, 409 duplicate username.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest input) {
        return ResponseEntity.created(URI.create("/api/me")).body(auth.register(input));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with username and password", description = "200 on success, 400 invalid input, 401 incorrect credentials.")
    public AuthResponse login(@Valid @RequestBody AuthRequest input) { return auth.login(input); }
}
