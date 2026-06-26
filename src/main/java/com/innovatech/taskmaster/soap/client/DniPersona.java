package com.innovatech.taskmaster.soap.client;

public record DniPersona(
    String dni,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    String nombreCompleto,
    String estado
) {
}
