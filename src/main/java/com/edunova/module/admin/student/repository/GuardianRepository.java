package com.edunova.module.admin.student.repository;


import com.edunova.module.admin.student.entity.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GuardianRepository extends JpaRepository<StudentGuardian, UUID> {


}