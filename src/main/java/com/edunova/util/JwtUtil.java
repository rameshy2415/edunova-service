package com.edunova.util;


import com.edunova.module.superadmin.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long      accessTokenExpiryMs;
    private final long      refreshTokenExpiryMs;
    private final long      setPasswordTokenExpiryMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${app.jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs,
            @Value("${app.jwt.set-password-token-expiry-ms}") long setPasswordTokenExpiryMs
    ) {
        this.signingKey           = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(secret.getBytes())
        ));
        this.accessTokenExpiryMs  = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
        this.setPasswordTokenExpiryMs = setPasswordTokenExpiryMs;
    }

    // ── Token generation ──────────────────────────────────────

    public String generateAccessToken(User user) {
        return buildToken(user, accessTokenExpiryMs, "access");
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenExpiryMs, "refresh");
    }

    public String generateSetPasswordToken(User user) {
        return buildToken(user, setPasswordTokenExpiryMs, "setPassword");
    }

    private String buildToken(User user, long expiryMs, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);
//        return Jwts.builder()
//                .subject(user.getId().toString())
//                .claim("username",    user.getEmail())
//                .claim("role",     user.getRole())
//                .claim("schoolId", user.getSchool() != null ? user.getSchool().getId().toString() : null)
//                .claim("type",     type)
//                .issuedAt(now)
//                .expiration(expiry)
//                .signWith(signingKey)
//                .compact();
        return Jwts.builder().issuer("EduNova App")
                .subject("JWT Token")
                .claim("id", user.getId() != null ? user.getId().toString() : null)
                .claim("email", user.getEmail())
                .claim("username",    user.getEmail())
                .claim("role", user.getRole())
                .claim("schoolId", user.getSchool() != null ? user.getSchool().getId().toString() : null)
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // ── Token validation ──────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT: {}", ex.getMessage());
        }
        return false;
    }

    public Claims validateAndExtractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(String token) {
        try {
            String type = parseClaims(token).get("type", String.class);
            return "refresh".equals(type);
        } catch (JwtException e) {
            return false;
        }
    }

    // ── Claims extraction ─────────────────────────────────────

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public String extractUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    public long getAccessTokenExpiryMs() {
        return accessTokenExpiryMs;
    }

    // ── Private helpers ───────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
