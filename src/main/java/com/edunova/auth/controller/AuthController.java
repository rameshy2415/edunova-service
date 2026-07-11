package com.edunova.auth.controller;


import com.edunova.auth.dto.AuthRequest;
import com.edunova.auth.service.AuthService;
import com.edunova.common.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Authentication", description = "Login, token refresh, password management")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    //private final JwtUtil jwtUtil;

    // ── POST /auth/login ───────────────────────────────────────

    @Operation(summary = "Login — all roles",
            description = "Authenticate with email + password + role. Returns JWT access & refresh tokens.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── POST /auth/refresh ─────────────────────────────────────

    @Operation(summary = "Refresh access token",
            description = "Exchange a valid refresh token for a new access + refresh token pair.")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    // ── POST /auth/forgot-password ─────────────────────────────

    @Operation(summary = "Forgot password — send reset email")
    @PostMapping("/forgot-password")
    public ResponseEntity<NormalApiResponse> forgotPassword(
            @RequestBody Map<String, String> body) {
        authService.forgotPassword(body.get("email"));
        return ResponseEntity.ok(NormalApiResponse.ok("If that email is registered, a reset link has been sent."));
    }


    // ── POST /auth/set-password ──────────────────────────────

    @Operation(summary = "Set your password using token from email")
    @PostMapping("/set-password")
    public ResponseEntity<AuthResponse> setPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.setPassword(request.token(), request.newPassword()));
    }


    // ── POST /auth/validate-token ──────────────────────────────

    @Operation(summary = "Validate token")
    @GetMapping("/validate-token")
    public ResponseEntity<AuthResponse> validateToken(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.validateToken(token));
    }

    // ── POST /auth/reset-password ──────────────────────────────

    @Operation(summary = "Reset password using token from email")
    @PostMapping("/reset-password")
    public ResponseEntity<NormalApiResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(NormalApiResponse.ok("Password updated successfully."));
    }

    // ── POST /auth/logout ──────────────────────────────────────

    @Operation(summary = "Logout (client-side token discard)")
    @PostMapping("/logout")
    public ResponseEntity<NormalApiResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        // Stateless JWT — client discards tokens.
        // Add token blacklist here if needed.
        SecurityContextHolder.clearContext();
        new SecurityContextLogoutHandler().logout(request, response, null);
        return ResponseEntity.ok(NormalApiResponse.ok("Logged out successfully."));
    }

    // ── GET /auth/me ───────────────────────────────────────────

    @Operation(summary = "Get current authenticated user's profile")
    @GetMapping("/me")
    public ResponseEntity<UserSummaryResponse> me(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }
}
