package com.edunova.module.superadmin.repository;

import com.edunova.module.superadmin.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    List<SubscriptionPlan> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<SubscriptionPlan> findByNameIgnoreCase(String name);
}
