package com.edunova.module.superadmin.repository;

import com.edunova.enums.UserRole;
import com.edunova.module.superadmin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ── UserRepository ─────────────────────────────────────────────────
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndSchool_Id(String email, UUID schoolId);

    Optional<User> findByEmailAndSchoolIsNull(String email);  // superadmin lookup

    Optional<User> findByEmail(String email);//School Admin

    Optional<User> findByPasswordResetToken(String token);

    boolean existsByEmailAndSchool_Id(String email, UUID schoolId);

    List<User> findBySchool_IdAndRole(UUID schoolId, UserRole role);

    @Query("SELECT u FROM User u WHERE u.school.id = :schoolId AND u.role = 'SCHOOL_ADMIN'")
    List<User> findAdminsBySchoolId(@Param("schoolId") UUID schoolId);

    @Query("SELECT count(u) FROM User u WHERE  u.role = :role")
    long countByRole(@Param("role") String role);

    @Query("SELECT count(u) FROM User u WHERE u.school.id = :schoolId AND u.role = :role")
    long countBySchool_IdAndRole(@Param("schoolId") UUID schoolId, @Param("role") String role);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

}
