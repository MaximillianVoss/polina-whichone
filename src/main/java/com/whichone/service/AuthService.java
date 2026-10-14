package com.whichone.service;

import com.whichone.domain.AppUser;
import com.whichone.dto.ApiDtos.*;
import com.whichone.exception.ApiException;
import com.whichone.repository.UserRepository;
import com.whichone.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@Transactional
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwords, TokenService tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.dummyHash = passwords.encode("Dummy-account-password-2026");
    }

    public AuthResponse register(AuthRequest request) {
        checkPasswordBytes(request.password());
        String username = normalize(request.username());
        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already registered.");
        }
        // No role or author ID is accepted. Every registration creates an ordinary user.
        var user = users.saveAndFlush(new AppUser(username, passwords.encode(request.password())));
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        checkPasswordBytes(request.password());
        var user = users.findByUsername(normalize(request.username()));
        boolean valid = passwords.matches(request.password(), user.map(AppUser::getPasswordHash).orElse(dummyHash));
        if (user.isEmpty() || !valid) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }
        return response(user.get());
    }

    @Transactional(readOnly = true)
    public AppUser requireUser(String username) {
        if (username == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return users.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account is unavailable."));
    }

    @Transactional(readOnly = true)
    public UserView me(String username) { return view(requireUser(username)); }

    private AuthResponse response(AppUser user) {
        return new AuthResponse(tokens.issue(user.getUsername()), "Bearer", tokens.getTtlSeconds(), view(user));
    }

    private UserView view(AppUser user) {
        return new UserView(user.getId(), user.getUsername(), user.getCreatedAt());
    }

    private String normalize(String username) { return username.trim().toLowerCase(Locale.ROOT); }

    private void checkPasswordBytes(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes for BCrypt.");
        }
    }
}
