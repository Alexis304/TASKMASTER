package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.dto.TareaCreateRequest;
import com.innovatech.taskmaster.dto.TareaResponse;
import com.innovatech.taskmaster.model.EstadoTarea;
import com.innovatech.taskmaster.model.Proyecto;
import com.innovatech.taskmaster.model.Tarea;
import com.innovatech.taskmaster.model.Usuario;
import com.innovatech.taskmaster.repository.ProyectoRepository;
import com.innovatech.taskmaster.repository.TareaRepository;
import com.innovatech.taskmaster.repository.UsuarioRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class TareaService {

    private final TareaRepository tareaRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HolidayValidationService holidayValidationService;
    private final NotificationService notificationService;

    public TareaService(
        TareaRepository tareaRepository,
        ProyectoRepository proyectoRepository,
        UsuarioRepository usuarioRepository,
        HolidayValidationService holidayValidationService,
        NotificationService notificationService
    ) {
        this.tareaRepository = tareaRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.holidayValidationService = holidayValidationService;
        this.notificationService = notificationService;
    }

    public TareaResponse crearTarea(TareaCreateRequest request) {
        Proyecto proyecto = proyectoRepository.findById(request.proyectoId())
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        Usuario usuario = usuarioRepository.findById(request.usuarioAsignadoId())
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Tarea tarea = new Tarea();
        tarea.setTitulo(request.titulo());
        tarea.setDescripcion(request.descripcion());
        tarea.setFechaLimite(request.fechaLimite());
        tarea.setProyecto(proyecto);
        tarea.setUsuarioAsignado(usuario);

        Tarea guardada = tareaRepository.save(tarea);
        String advertencia = holidayValidationService.generarAdvertencia(request.fechaLimite());

        notificationService.notificarNuevaTarea(guardada);

        return toResponse(guardada, advertencia);
    }

    public List<TareaResponse> listarTareas() {
        return tareaRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
            .stream()
            .map(tarea -> toResponse(tarea, holidayValidationService.generarAdvertencia(tarea.getFechaLimite())))
            .toList();
    }

    public TareaResponse actualizarEstado(Long id, EstadoTarea estado) {
        Tarea tarea = tareaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
        tarea.setEstado(estado);
        Tarea actualizada = tareaRepository.save(tarea);
        return toResponse(actualizada, holidayValidationService.generarAdvertencia(actualizada.getFechaLimite()));
    }

    public void eliminar(Long id) {
        if (!tareaRepository.existsById(id)) {
            throw new IllegalArgumentException("Tarea no encontrada");
        }
        tareaRepository.deleteById(id);
    }

    private TareaResponse toResponse(Tarea tarea, String advertenciaFecha) {
        return new TareaResponse(
            tarea.getId(),
            tarea.getTitulo(),
            tarea.getDescripcion(),
            tarea.getFechaLimite(),
            tarea.getEstado(),
            tarea.getProyecto().getId(),
            tarea.getProyecto().getNombre(),
            tarea.getUsuarioAsignado().getId(),
            tarea.getUsuarioAsignado().getNombres(),
            advertenciaFecha
        );
    }
}
