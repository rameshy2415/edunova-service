package com.edunova.module.superadmin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"school_id", "email"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** NULL for superadmin — they are not scoped to a school. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    @Column
    private String role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_first_time_login", nullable = false)
    @Builder.Default
    private Boolean isFirstTimeLogin = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_reset_token", columnDefinition = "TEXT")
    private String passwordResetToken;

    @Column(name = "password_reset_expires")
    private Instant passwordResetExpires;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
