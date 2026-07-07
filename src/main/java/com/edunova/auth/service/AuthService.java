package com.edunova.auth.service;


import com.edunova.auth.dto.AuthRequest;
import com.edunova.common.dto.AuthResponse;
import com.edunova.common.dto.UserSummaryResponse;
import com.edunova.exception.BusinessException;
import com.edunova.module.admin.student.repository.AcademicYearRepository;
import com.edunova.module.superadmin.entity.User;
import com.edunova.module.superadmin.repository.UserRepository;
import com.edunova.notification.email.EmailNotificationService;
import com.edunova.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailNotificationService emailService;
    private final AcademicYearRepository academicYearRepository;

    // ── Login ──────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(AuthRequest.LoginRequest request) {
        // Look up user by email — superadmin has no schoolId
        User user;
//        if ("superadmin".equalsIgnoreCase(request.role())) {
//            user = userRepository.findByEmailAndSchoolIsNull(request.email())
//                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
//        } else {
//            // For school-scoped roles, email is unique per school but not across schools.
//            // Frontend passes role; we find the first matching active user.
//            user = userRepository
//                    .findAll()
//                    .stream()
//                    .filter(u -> u.getEmail().equalsIgnoreCase(request.email())
//                            && u.getRole().name().equalsIgnoreCase(request.role()))
//                    .findFirst()
//                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
//        }
        user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.getIsActive()) {
            throw new BusinessException("ACCOUNT_DISABLED",
                    "This account has been disabled. Contact your administrator.");
        }

        // Verify password
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword()));

        // Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    // ── Refresh token ──────────────────────────────────────────

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException("INVALID_TOKEN", "Refresh token is invalid or expired.");
        }
        //UUID userId = jwtUtil.extractUserId(refreshToken);
        String email= jwtUtil.extractEmail(refreshToken);
        User user   = userRepository.findByEmailAndSchoolIsNull(email).orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found."));

        if (!user.getIsActive()) {
            throw new BusinessException("ACCOUNT_DISABLED", "Account is disabled.");
        }
        return buildAuthResponse(user);
    }

    // ── Forgot password ────────────────────────────────────────

    @Transactional
    public void forgotPassword(String email) {
        // Silent — do not reveal whether account exists
        userRepository.findAll().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .ifPresent(user -> {
                    //String token = UUID.randomUUID().toString();//TODO Need to generate actual Token

                    // 2. Create admin user — NO password; generate 2-hour set-password token
                    String setPasswordToken =  jwtUtil.generateSetPasswordToken(user);
                    user.setPasswordResetToken(setPasswordToken);
                    user.setPasswordResetExpires(Instant.now().plusSeconds(7200)); // 2 hour
                    userRepository.save(user);

                    var userName = user.getFirstName()+" "+ user.getLastName();
                    emailService.sendPasswordResetEmail(user.getEmail(),userName, setPasswordToken);
                });
    }

    // ── Reset password ─────────────────────────────────────────

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN",
                        "Reset token is invalid or has already been used."));

        if (user.getPasswordResetExpires() == null
                || user.getPasswordResetExpires().isBefore(Instant.now())) {
            throw new BusinessException("TOKEN_EXPIRED",
                    "Reset token has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        userRepository.save(user);
        log.info("Password reset successful for user {}", user.getEmail());
    }

    @Transactional
    public AuthResponse setPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN",
                        "Reset token is invalid or has already been used."));

        if (user.getPasswordResetExpires() == null
                || user.getPasswordResetExpires().isBefore(Instant.now())) {
            throw new BusinessException("TOKEN_EXPIRED",
                    "Reset token has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        user.setIsFirstTimeLogin(false);
        userRepository.save(user);
        log.info("Password set successful for user {}", user.getEmail());
        return buildAuthResponse(user);

    }

    @Transactional
    public AuthResponse validateToken(String token) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN",
                        "Reset token is invalid or has already been used."));

        if (user.getPasswordResetExpires() == null
                || user.getPasswordResetExpires().isBefore(Instant.now())) {
            throw new BusinessException("TOKEN_EXPIRED",
                    "Reset token has expired. Please request a new one.");
        }

        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException("INVALID_TOKEN", "Refresh token is invalid or expired.");
        }
        //UUID userId = jwtUtil.extractUserId(refreshToken);
        String email= jwtUtil.extractEmail(token);
        //User user1   = userRepository.findByEmailAndSchoolIsNull(email).orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found."));

        if (!user.getIsActive()) {
            throw new BusinessException("ACCOUNT_DISABLED", "Account is disabled.");
        }
        return buildAuthUserResponse(user);
    }

    // ── Get current user ───────────────────────────────────────

    public UserSummaryResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found."));
        return toSummary(user);
    }

    // ── Private helpers ────────────────────────────────────────

    private AuthResponse buildAuthUserResponse(User user) {
        return new AuthResponse(
                null,
                null,
                null,
                0,
                toSummary(user)
        );
    }


    private AuthResponse buildAuthResponse(User user) {
        String access  = jwtUtil.generateAccessToken(user);
        String refresh = jwtUtil.generateRefreshToken(user);
        return new AuthResponse(
                access,
                refresh,
                AuthResponse.TOKEN_TYPE,
                jwtUtil.getAccessTokenExpiryMs() / 1000,
                toSummary(user)
        );
    }

    private UserSummaryResponse toSummary(User user) {

        var expiryAt = user.getPasswordResetExpires();


        //TODO need to  find out better way to set the School details to the claim
        UUID academicYearId= null;
        if(user.getSchool() != null){
            academicYearId =  academicYearRepository.findIdBySchoolIdAndIsCurrentTrue(user.getSchool().getId());
        }

        return new UserSummaryResponse(
                user.getId(),
                user.getFirstName()+ " " +user.getLastName(),       // name field — replace with a name column if added
                user.getEmail(),
                user.getRole(),
                user.getSchool() != null ? user.getSchool().getId()   : null,
                user.getSchool() != null ? user.getSchool().getName() : null,
                academicYearId,
                user.getAvatarUrl(),
                user.getIsFirstTimeLogin(),
                (expiryAt == null) ? null :  LocalDateTime.ofInstant(user.getPasswordResetExpires(), ZoneId.systemDefault())
        );
    }
}
