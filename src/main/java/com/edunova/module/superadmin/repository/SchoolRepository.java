package com.edunova.module.superadmin.repository;

import com.edunova.module.superadmin.entity.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// ── SchoolRepository ───────────────────────────────────────────────
@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {

/*    @Query("""
        SELECT s FROM School s
        WHERE (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(s.city) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:isActive IS NULL OR s.isActive = :isActive)
        """)
    Page<School> findAllFiltered(
            @Param("search")   String  search,
            @Param("isActive") Boolean isActive,
            Pageable pageable);*/

    @Query("""
        SELECT s FROM School s
        WHERE :isActive IS NULL OR s.isActive = :isActive
        """)
    Page<School> findAllFiltered(@Param("search")   String  search, @Param("isActive") Boolean isActive, Pageable pageable);

    boolean existsByEmail(String email);
}
