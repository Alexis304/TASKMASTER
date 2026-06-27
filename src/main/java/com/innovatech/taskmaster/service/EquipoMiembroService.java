package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.dto.EquipoMiembroRequest;
import com.innovatech.taskmaster.dto.EquipoMiembroResponse;
import com.innovatech.taskmaster.model.EquipoMiembro;
import com.innovatech.taskmaster.model.Proyecto;
import com.innovatech.taskmaster.model.Usuario;
import com.innovatech.taskmaster.repository.EquipoMiembroRepository;
import com.innovatech.taskmaster.repository.ProyectoRepository;
import com.innovatech.taskmaster.repository.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EquipoMiembroService {

    private final EquipoMiembroRepository equipoMiembroRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;

    public EquipoMiembroService(
        EquipoMiembroRepository equipoMiembroRepository,
        ProyectoRepository proyectoRepository,
        UsuarioRepository usuarioRepository
    ) {
        this.equipoMiembroRepository = equipoMiembroRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<EquipoMiembroResponse> listar(Long proyectoId) {
        List<EquipoMiembro> miembros = proyectoId == null
            ? equipoMiembroRepository.findAllByOrderByProyecto_NombreAscUsuario_NombresAsc()
            : equipoMiembroRepository.findByProyectoIdOrderByUsuario_NombresAsc(proyectoId);

        return miembros.stream().map(this::toResponse).toList();
    }

    public EquipoMiembroResponse asociar(EquipoMiembroRequest request) {
        if (equipoMiembroRepository.existsByProyectoIdAndUsuarioId(request.proyectoId(), request.usuarioId())) {
            throw new IllegalArgumentException("Este usuario ya esta asociado al proyecto.");
        }

        Proyecto proyecto = proyectoRepository.findById(request.proyectoId())
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        EquipoMiembro miembro = new EquipoMiembro();
        miembro.setProyecto(proyecto);
        miembro.setUsuario(usuario);
        return toResponse(equipoMiembroRepository.save(miembro));
    }

    public EquipoMiembroResponse actualizar(Long id, EquipoMiembroRequest request) {
        EquipoMiembro miembro = equipoMiembroRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asociacion no encontrada"));

        if (
            !miembro.getProyecto().getId().equals(request.proyectoId()) ||
            !miembro.getUsuario().getId().equals(request.usuarioId())
        ) {
            if (equipoMiembroRepository.existsByProyectoIdAndUsuarioId(request.proyectoId(), request.usuarioId())) {
                throw new IllegalArgumentException("Este usuario ya esta asociado al proyecto.");
            }
        }

        Proyecto proyecto = proyectoRepository.findById(request.proyectoId())
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        miembro.setProyecto(proyecto);
        miembro.setUsuario(usuario);
        return toResponse(equipoMiembroRepository.save(miembro));
    }

    public void eliminar(Long id) {
        if (!equipoMiembroRepository.existsById(id)) {
            throw new IllegalArgumentException("Asociacion no encontrada");
        }

        equipoMiembroRepository.deleteById(id);
    }

    private EquipoMiembroResponse toResponse(EquipoMiembro miembro) {
        Usuario usuario = miembro.getUsuario();
        Proyecto proyecto = miembro.getProyecto();
        return new EquipoMiembroResponse(
            miembro.getId(),
            proyecto.getId(),
            proyecto.getNombre(),
            usuario.getId(),
            usuario.getNombres(),
            usuario.getEmail(),
            usuario.getFotoUrl()
        );
    }
}
