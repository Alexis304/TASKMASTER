package com.innovatech.taskmaster.dto;

public record EquipoMiembroResponse(
    Long id,
    Long proyectoId,
    String proyectoNombre,
    Long usuarioId,
    String usuarioNombre,
    String usuarioEmail,
    String usuarioFotoUrl
) {
}
