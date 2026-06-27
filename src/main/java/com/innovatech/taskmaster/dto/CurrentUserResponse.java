package com.innovatech.taskmaster.dto;

public record CurrentUserResponse(
    Long id,
    String email,
    String nombres,
    String fotoUrl
) {
}
