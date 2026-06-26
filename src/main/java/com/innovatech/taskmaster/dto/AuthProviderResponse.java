package com.innovatech.taskmaster.dto;

public record AuthProviderResponse(
    boolean googleEnabled,
    String googleAuthUrl
) {
}
