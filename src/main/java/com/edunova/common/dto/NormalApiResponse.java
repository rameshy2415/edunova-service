package com.edunova.common.dto;

public record NormalApiResponse(
        boolean success,
        String  message
) {
    public static NormalApiResponse ok(String message) {
        return new NormalApiResponse(true, message);
    }
    public static NormalApiResponse error(String message) {
        return new NormalApiResponse(false, message);
    }
}
