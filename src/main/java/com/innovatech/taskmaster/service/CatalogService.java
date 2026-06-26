package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.dto.ProyectoOptionResponse;
import com.innovatech.taskmaster.dto.UsuarioOptionResponse;
import com.innovatech.taskmaster.repository.ProyectoRepository;
import com.innovatech.taskmaster.repository.UsuarioRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;

    public CatalogService(ProyectoRepository proyectoRepository, UsuarioRepository usuarioRepository) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<ProyectoOptionResponse> listarProyectos() {
        return proyectoRepository.findAll(Sort.by("nombre"))
            .stream()
            .map(proyecto -> new ProyectoOptionResponse(proyecto.getId(), proyecto.getNombre()))
            .toList();
    }

    public List<UsuarioOptionResponse> listarUsuarios() {
        return usuarioRepository.findAll(Sort.by("nombres"))
            .stream()
            .map(usuario -> new UsuarioOptionResponse(usuario.getId(), usuario.getNombres(), usuario.getEmail()))
            .toList();
    }
}
