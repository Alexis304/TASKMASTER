package com.innovatech.taskmaster.dto;

import jakarta.validation.constraints.NotNull;

public record EquipoMiembroRequest(
    @NotNull Long proyectoId,
    @NotNull Long usuarioId
) {
}
