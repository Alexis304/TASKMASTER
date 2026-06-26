package com.innovatech.taskmaster.dto;

import com.innovatech.taskmaster.model.EstadoTarea;
import java.time.LocalDate;

public record TareaResponse(
    Long id,
    String titulo,
    String descripcion,
    LocalDate fechaLimite,
    EstadoTarea estado,
    Long proyectoId,
    String proyectoNombre,
    Long usuarioAsignadoId,
    String usuarioAsignadoNombre,
    String advertenciaFecha
) {
}
