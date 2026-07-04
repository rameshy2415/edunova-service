package com.edunova.filter;

import com.edunova.module.superadmin.model.UserSchoolDTO;


public class LoggedInUserContextDetails {
    private static final ThreadLocal<UserSchoolDTO> currentUser = new ThreadLocal<>();

    public static void setCurrentUser(UserSchoolDTO user) {
        currentUser.set(user);
    }

    public static UserSchoolDTO getCurrentUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
