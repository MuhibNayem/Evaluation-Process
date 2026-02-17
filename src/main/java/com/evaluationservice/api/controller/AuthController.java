package com.evaluationservice.api.controller;

import com.evaluationservice.api.dto.request.LoginRequest;
import com.evaluationservice.api.dto.response.LoginResponse;
import com.evaluationservice.infrastructure.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for authentication endpoints.
 * Provides mock login functionality for development.
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(prefix = "evaluation.service.security", name = "dev-mode", havingValue = "true")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Mock authentication logic
        String username = request.username();
        String password = request.password();
        List<String> roles;

        if ("admin".equals(username) && "admin".equals(password)) {
            roles = List.of("ROLE_ADMIN", "ROLE_EVALUATOR"); // Admin needs evaluator role too? Maybe not.
        } else if ("evaluator".equals(username) && "evaluator".equals(password)) {
            roles = List.of("ROLE_EVALUATOR");
        } else if ("evaluatee".equals(username) && "evaluatee".equals(password)) {
            roles = List.of("ROLE_EVALUATEE");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtUtil.generateToken(username, roles);
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
