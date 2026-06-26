package com.innovatech.taskmaster.controller;

import com.innovatech.taskmaster.dto.TareaCreateRequest;
import com.innovatech.taskmaster.dto.TareaResponse;
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
    public ResponseEntity<List<TareaResponse>> listar() {
        return ResponseEntity.ok(tareaService.listarTareas());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TareaResponse> actualizarEstado(@PathVariable Long id, @RequestParam EstadoTarea estado) {
        return ResponseEntity.ok(tareaService.actualizarEstado(id, estado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tareaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
