package com.edunova.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    SUPER_ADMIN,
    SCHOOL_ADMIN,
    TEACHER,
    STUDENT,
    PARENT

   /* SUPER_ADMIN("superadmin"),
    SCHOOL_ADMIN("admin"),
    TEACHER("teacher"),
    STUDENT("student"),
    PARENT("parent");
    private String value;
    UserRole(String value){
        this.value= value;
    }*/
}
