package com.innovatech.taskmaster.controller;

import com.innovatech.taskmaster.dto.TareaCreateRequest;
import com.innovatech.taskmaster.dto.TareaResponse;
import com.innovatech.taskmaster.dto.TareaUpdateRequest;
import com.innovatech.taskmaster.model.EstadoTarea;
import com.innovatech.taskmaster.service.TareaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tareas")
public class TareaController {

    private final TareaService tareaService;

    public TareaController(TareaService tareaService) {
        this.tareaService = tareaService;
    }

    @PostMapping
    public ResponseEntity<TareaResponse> crear(@Valid @RequestBody TareaCreateRequest request) {
        return ResponseEntity.ok(tareaService.crearTarea(request));
    }

    @GetMapping
    public ResponseEntity<List<TareaResponse>> listar(
        @RequestParam(required = false) EstadoTarea estado,
        @RequestParam(required = false) Long proyectoId,
        @RequestParam(required = false) Long usuarioId,
        @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(tareaService.listarTareas(estado, proyectoId, usuarioId, q));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TareaResponse> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) TareaUpdateRequest request,
        @RequestParam(required = false) EstadoTarea estado
    ) {
        if (request != null) {
            return ResponseEntity.ok(tareaService.actualizarTarea(id, request));
        }
        if (estado != null) {
            return ResponseEntity.ok(tareaService.actualizarEstado(id, estado));
        }
        throw new IllegalArgumentException("Debes enviar un cuerpo valido o el parametro estado.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tareaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
