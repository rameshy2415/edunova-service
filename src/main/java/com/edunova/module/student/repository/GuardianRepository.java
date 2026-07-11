package com.edunova.module.student.repository;


import com.edunova.module.student.entity.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuardianRepository extends JpaRepository<StudentGuardian, UUID> {

    @Query("""
            select g from StudentGuardian g
            where g.studentId= :studentId
            """)
    Optional<StudentGuardian> findParent(@Param("studentId") UUID studentId);


}