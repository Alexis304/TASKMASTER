package com.innovatech.taskmaster.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TareaCreateRequest(
    @NotBlank String titulo,
    String descripcion,
    @NotNull @FutureOrPresent LocalDate fechaLimite,
    @NotNull Long proyectoId,
    @NotNull Long usuarioAsignadoId
) {
}
