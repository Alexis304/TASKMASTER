package com.innovatech.taskmaster.dto;

import com.innovatech.taskmaster.model.EstadoTarea;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TareaUpdateRequest(
    @NotBlank String titulo,
    String descripcion,
    @NotNull @FutureOrPresent LocalDate fechaLimite,
    @NotNull EstadoTarea estado,
    @NotNull Long proyectoId,
    @NotNull Long usuarioAsignadoId
) {
}
