package com.edunova.module.superadmin.repository;

import com.edunova.module.superadmin.entity.SchoolOnboardingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// ── SchoolOnboardingLogRepository ────────────────────────────────
@Repository
public interface SchoolOnboardingLogRepository extends JpaRepository<SchoolOnboardingLog, UUID> {

    List<SchoolOnboardingLog> findBySchool_IdOrderByOnboardedAtDesc(UUID schoolId);
}
