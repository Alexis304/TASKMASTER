package com.innovatech.taskmaster.controller;

import com.innovatech.taskmaster.dto.ProyectoRequest;
import com.innovatech.taskmaster.dto.ProyectoResponse;
import com.innovatech.taskmaster.service.ProyectoService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proyectos")
public class ProyectoController {

    private final ProyectoService proyectoService;

    public ProyectoController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    @GetMapping
    public ResponseEntity<List<ProyectoResponse>> listar() {
        return ResponseEntity.ok(proyectoService.listar());
    }

    @PostMapping
    public ResponseEntity<ProyectoResponse> crear(@Valid @RequestBody ProyectoRequest request) {
        return ResponseEntity.ok(proyectoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProyectoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ProyectoRequest request) {
        return ResponseEntity.ok(proyectoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        proyectoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
