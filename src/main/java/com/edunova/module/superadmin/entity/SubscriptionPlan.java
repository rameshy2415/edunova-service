package com.edunova.module.superadmin.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// ─────────────────────────────────────────────
//  SubscriptionPlan — master plan catalogue
// ─────────────────────────────────────────────
@Entity
@Table(name = "subscription_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;                    // "Basic", "Pro", "Enterprise"

    @Column(name = "max_students")
    private Integer maxStudents;            // NULL = unlimited

    @Column(name = "max_teachers")
    private Integer maxTeachers;

    @Column(name = "price_monthly", precision = 10, scale = 2)
    private BigDecimal priceMonthly;

    @Column(name = "price_annually", precision = 10, scale = 2)
    private BigDecimal priceAnnually;

    /** JSON array of feature keys e.g. ["attendance","fees","reports"] */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String features = "[]";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Short sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
