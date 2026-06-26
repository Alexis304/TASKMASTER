package com.innovatech.taskmaster.dto;

public record AuthResponse(
    boolean authenticated,
    String message
) {
}
