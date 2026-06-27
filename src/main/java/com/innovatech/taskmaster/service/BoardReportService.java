package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.dto.TareaResponse;
import com.innovatech.taskmaster.model.EstadoTarea;
import com.innovatech.taskmaster.soap.client.BoardReportPdf;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BoardReportService {

    private final TareaService tareaService;
    private final ReportSoapClient reportSoapClient;

    public BoardReportService(TareaService tareaService, ReportSoapClient reportSoapClient) {
        this.tareaService = tareaService;
        this.reportSoapClient = reportSoapClient;
    }

    public BoardReportPdf generarReporteTablero(
        EstadoTarea estado,
        Long proyectoId,
        Long usuarioId,
        String q,
        String generadoPor
    ) {
        List<TareaResponse> tareas = tareaService.listarTareas(estado, proyectoId, usuarioId, q);
        String contenido = construirContenido(tareas);
        String fechaGeneracion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return reportSoapClient.generarReporteTablero(
            "Reporte de tablero TaskMaster",
            generadoPor,
            fechaGeneracion,
            contenido
        );
    }

    private String construirContenido(List<TareaResponse> tareas) {
        if (tareas.isEmpty()) {
            return "No hay tareas para los filtros seleccionados.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Total de tareas: ").append(tareas.size()).append(System.lineSeparator()).append(System.lineSeparator());

        for (EstadoTarea estado : EstadoTarea.values()) {
            List<TareaResponse> tareasPorEstado = tareas.stream()
                .filter(tarea -> tarea.estado() == estado)
                .toList();

            builder.append(estado.name().replace("_", " "))
                .append(" (")
                .append(tareasPorEstado.size())
                .append(")")
                .append(System.lineSeparator());

            for (TareaResponse tarea : tareasPorEstado) {
                builder.append("- ")
                    .append(tarea.titulo())
                    .append(" | Proyecto: ")
                    .append(tarea.proyectoNombre())
                    .append(" | Responsable: ")
                    .append(tarea.usuarioAsignadoNombre())
                    .append(" | Fecha limite: ")
                    .append(tarea.fechaLimite())
                    .append(System.lineSeparator());

                if (tarea.descripcion() != null && !tarea.descripcion().isBlank()) {
                    builder.append("  ")
                        .append(tarea.descripcion())
                        .append(System.lineSeparator());
                }
            }

            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }
}
