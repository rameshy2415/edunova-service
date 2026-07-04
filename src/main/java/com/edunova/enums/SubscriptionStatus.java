package com.edunova.enums;

import lombok.Getter;

@Getter
public enum SubscriptionStatus {
    TRIAL("Trial"),
    ACTIVE("Active"),
    EXPIRED("Expired"),
    SUSPENDED("Suspended"),
    CANCELLED("Cancelled");

    private final String value;

    SubscriptionStatus(String value) {
        this.value = value;

    }

}
