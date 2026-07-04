package com.edunova.module.superadmin.entity;



import com.edunova.enums.BillingCycle;
import com.edunova.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    //@Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = SubscriptionStatus.TRIAL.getValue();

    //@Enumerated(EnumType.STRING)
//    @Column
//    @Builder.Default
//    private String status = SubscriptionStatus.TRIAL.name();

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 15)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.Annual;

    //@Enumerated(EnumType.STRING)
//    @Column(name = "billing_cycle")
//    @Builder.Default
//    private String billingCycle = BillingCycle.ANNUAL.name();

    /** Overrides plan price when set (for negotiated enterprise deals). */
    @Column(name = "amount_override", precision = 12, scale = 2)
    private BigDecimal amountOverride;

    @Column(name = "discount_pct", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal discountPct = BigDecimal.ZERO;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "current_period_start", nullable = false)
    private LocalDate currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private LocalDate currentPeriodEnd;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    /** The superadmin who created / last modified this subscription. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
