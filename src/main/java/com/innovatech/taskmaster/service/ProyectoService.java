package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.dto.ProyectoRequest;
import com.innovatech.taskmaster.dto.ProyectoResponse;
import com.innovatech.taskmaster.model.Proyecto;
import com.innovatech.taskmaster.repository.ProyectoRepository;
import com.innovatech.taskmaster.repository.TareaRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final TareaRepository tareaRepository;

    public ProyectoService(ProyectoRepository proyectoRepository, TareaRepository tareaRepository) {
        this.proyectoRepository = proyectoRepository;
        this.tareaRepository = tareaRepository;
    }

    public List<ProyectoResponse> listar() {
        return proyectoRepository.findAll(Sort.by("nombre"))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public ProyectoResponse crear(ProyectoRequest request) {
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre(request.nombre().trim());
        proyecto.setDescripcion(trimToNull(request.descripcion()));
        return toResponse(proyectoRepository.save(proyecto));
    }

    public ProyectoResponse actualizar(Long id, ProyectoRequest request) {
        Proyecto proyecto = proyectoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        proyecto.setNombre(request.nombre().trim());
        proyecto.setDescripcion(trimToNull(request.descripcion()));
        return toResponse(proyectoRepository.save(proyecto));
    }

    public void eliminar(Long id) {
        if (!proyectoRepository.existsById(id)) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }

        if (tareaRepository.existsByProyectoId(id)) {
            throw new IllegalArgumentException("No se puede eliminar un proyecto con tareas asociadas.");
        }

        proyectoRepository.deleteById(id);
    }

    private ProyectoResponse toResponse(Proyecto proyecto) {
        return new ProyectoResponse(proyecto.getId(), proyecto.getNombre(), proyecto.getDescripcion());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
