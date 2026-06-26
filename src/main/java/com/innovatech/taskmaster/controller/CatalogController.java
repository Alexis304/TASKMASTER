package com.innovatech.taskmaster.controller;

import com.innovatech.taskmaster.dto.ProyectoOptionResponse;
import com.innovatech.taskmaster.dto.UsuarioOptionResponse;
import com.innovatech.taskmaster.service.CatalogService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/proyectos")
    public ResponseEntity<List<ProyectoOptionResponse>> listarProyectos() {
        return ResponseEntity.ok(catalogService.listarProyectos());
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioOptionResponse>> listarUsuarios() {
        return ResponseEntity.ok(catalogService.listarUsuarios());
    }
}
