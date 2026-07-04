package com.edunova.module.superadmin.repository;


import com.edunova.enums.SubscriptionStatus;
import com.edunova.module.superadmin.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ── SubscriptionRepository ────────────────────────────────────────
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findBySchool_Id(UUID schoolId);

    @Query("""
        SELECT s FROM Subscription s
        WHERE (:status IS NULL OR s.status = :status)
          AND (:planId IS NULL OR s.plan.id = :planId)
        ORDER BY s.createdAt DESC
        """)
    Page<Subscription> findAllFiltered(
            @Param("status") SubscriptionStatus status,
            @Param("planId") UUID planId,
            Pageable pageable);

    /** Find subscriptions expiring within the next N days — used by scheduler. */
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.status = 'ACTIVE'
          AND s.currentPeriodEnd <= :expiryThreshold
        """)
    List<Subscription> findExpiringBefore(@Param("expiryThreshold") LocalDate expiryThreshold);

    /** Find expired trials that have not been converted. */
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.status = 'TRIAL'
          AND s.trialEndsAt < CURRENT_TIMESTAMP
        """)
    List<Subscription> findExpiredTrials();

    @Query("""
        SELECT count(s) FROM Subscription s
        WHERE s.status = :status
        """)
    int countByStatus(@Param("status") SubscriptionStatus status);
}
