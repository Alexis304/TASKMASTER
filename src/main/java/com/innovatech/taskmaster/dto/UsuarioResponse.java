package com.innovatech.taskmaster.dto;

public record UsuarioResponse(
    Long id,
    String email,
    String dni,
    String nombres
) {
}
