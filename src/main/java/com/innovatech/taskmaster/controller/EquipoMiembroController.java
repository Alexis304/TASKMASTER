package com.innovatech.taskmaster.controller;

import com.innovatech.taskmaster.dto.EquipoMiembroRequest;
import com.innovatech.taskmaster.dto.EquipoMiembroResponse;
import com.innovatech.taskmaster.service.EquipoMiembroService;
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
@RequestMapping("/api/equipos")
public class EquipoMiembroController {

    private final EquipoMiembroService equipoMiembroService;

    public EquipoMiembroController(EquipoMiembroService equipoMiembroService) {
        this.equipoMiembroService = equipoMiembroService;
    }

    @GetMapping
    public ResponseEntity<List<EquipoMiembroResponse>> listar(@RequestParam(required = false) Long proyectoId) {
        return ResponseEntity.ok(equipoMiembroService.listar(proyectoId));
    }

    @PostMapping
    public ResponseEntity<EquipoMiembroResponse> asociar(@Valid @RequestBody EquipoMiembroRequest request) {
        return ResponseEntity.ok(equipoMiembroService.asociar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipoMiembroResponse> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody EquipoMiembroRequest request
    ) {
        return ResponseEntity.ok(equipoMiembroService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        equipoMiembroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
