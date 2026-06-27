package com.innovatech.taskmaster.controller;

import com.innovatech.taskmaster.dto.DniLookupResponse;
import com.innovatech.taskmaster.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dni")
public class DniController {

    private final UsuarioService usuarioService;

    public DniController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/{dni}")
    public ResponseEntity<DniLookupResponse> consultar(@PathVariable String dni) {
        return ResponseEntity.ok(usuarioService.consultarDni(dni));
    }
}
