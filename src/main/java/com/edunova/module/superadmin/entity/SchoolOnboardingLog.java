package com.edunova.module.superadmin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

// ─────────────────────────────────────────────
//  SchoolOnboardingLog
// ─────────────────────────────────────────────
@Entity
@Table(name = "school_onboarding_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchoolOnboardingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "onboarded_by", nullable = false)
    private User onboardedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private User adminUser;

    @Column(name = "admin_email", nullable = false, length = 150)
    private String adminEmail;

    @Column(name = "admin_name", nullable = false, length = 150)
    private String adminName;

    @Column(name = "welcome_email_sent", nullable = false)
    @Builder.Default
    private Boolean welcomeEmailSent = false;

    @Column(name = "welcome_email_sent_at")
    private Instant welcomeEmailSentAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "onboarded_at", updatable = false)
    private Instant onboardedAt;
}
