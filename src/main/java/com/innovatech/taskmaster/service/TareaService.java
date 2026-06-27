package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.dto.TareaCreateRequest;
import com.innovatech.taskmaster.dto.TareaResponse;
import com.innovatech.taskmaster.dto.TareaUpdateRequest;
import com.innovatech.taskmaster.model.EstadoTarea;
import com.innovatech.taskmaster.model.Proyecto;
import com.innovatech.taskmaster.model.Tarea;
import com.innovatech.taskmaster.model.Usuario;
import com.innovatech.taskmaster.repository.ProyectoRepository;
import com.innovatech.taskmaster.repository.TareaRepository;
import com.innovatech.taskmaster.repository.UsuarioRepository;
import com.innovatech.taskmaster.websocket.TaskRealtimeService;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class TareaService {

    private final TareaRepository tareaRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HolidayValidationService holidayValidationService;
    private final NotificationService notificationService;
    private final TaskRealtimeService taskRealtimeService;

    public TareaService(
        TareaRepository tareaRepository,
        ProyectoRepository proyectoRepository,
        UsuarioRepository usuarioRepository,
        HolidayValidationService holidayValidationService,
        NotificationService notificationService,
        TaskRealtimeService taskRealtimeService
    ) {
        this.tareaRepository = tareaRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.holidayValidationService = holidayValidationService;
        this.notificationService = notificationService;
        this.taskRealtimeService = taskRealtimeService;
    }

    public TareaResponse crearTarea(TareaCreateRequest request) {
        Tarea tarea = new Tarea();
        aplicarCambios(
            tarea,
            request.titulo(),
            request.descripcion(),
            request.fechaLimite(),
            EstadoTarea.PENDIENTE,
            request.proyectoId(),
            request.usuarioAsignadoId()
        );

        Tarea guardada = tareaRepository.save(tarea);
        String advertencia = holidayValidationService.generarAdvertencia(request.fechaLimite());

        notificationService.notificarNuevaTarea(guardada);

        TareaResponse response = toResponse(guardada, advertencia);
        taskRealtimeService.tareaCreada(response);
        return response;
    }

    public List<TareaResponse> listarTareas(EstadoTarea estado, Long proyectoId, Long usuarioId, String q) {
        return tareaRepository.findAll(buildFilters(estado, proyectoId, usuarioId, q), Sort.by(Sort.Direction.DESC, "id"))
            .stream()
            .map(tarea -> toResponse(tarea, holidayValidationService.generarAdvertencia(tarea.getFechaLimite())))
            .toList();
    }

    public TareaResponse actualizarEstado(Long id, EstadoTarea estado) {
        Tarea tarea = tareaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
        validarTareaEditable(tarea);
        tarea.setEstado(estado);
        Tarea actualizada = tareaRepository.save(tarea);
        TareaResponse response = toResponse(actualizada, holidayValidationService.generarAdvertencia(actualizada.getFechaLimite()));
        taskRealtimeService.tareaMovida(response);
        return response;
    }

    public TareaResponse actualizarTarea(Long id, TareaUpdateRequest request) {
        Tarea tarea = tareaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
        validarTareaEditable(tarea);

        aplicarCambios(
            tarea,
            request.titulo(),
            request.descripcion(),
            request.fechaLimite(),
            request.estado(),
            request.proyectoId(),
            request.usuarioAsignadoId()
        );

        Tarea actualizada = tareaRepository.save(tarea);
        TareaResponse response = toResponse(actualizada, holidayValidationService.generarAdvertencia(actualizada.getFechaLimite()));
        taskRealtimeService.tareaActualizada(response);
        return response;
    }

    public void eliminar(Long id) {
        Tarea tarea = tareaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
        validarTareaEditable(tarea);
        String titulo = tarea.getTitulo();
        tareaRepository.delete(tarea);
        taskRealtimeService.tareaEliminada(id, titulo);
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

    private void aplicarCambios(
        Tarea tarea,
        String titulo,
        String descripcion,
        java.time.LocalDate fechaLimite,
        EstadoTarea estado,
        Long proyectoId,
        Long usuarioAsignadoId
    ) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        Usuario usuario = usuarioRepository.findById(usuarioAsignadoId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        tarea.setTitulo(titulo.trim());
        tarea.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim());
        tarea.setFechaLimite(fechaLimite);
        tarea.setEstado(estado);
        tarea.setProyecto(proyecto);
        tarea.setUsuarioAsignado(usuario);
    }

    private void validarTareaEditable(Tarea tarea) {
        if (tarea.getEstado() == EstadoTarea.COMPLETADA) {
            throw new IllegalArgumentException("Las tareas en Hecho ya no se pueden modificar.");
        }
    }

    private Specification<Tarea> buildFilters(EstadoTarea estado, Long proyectoId, Long usuarioId, String q) {
        return (root, query, criteriaBuilder) -> {
            root.fetch("proyecto", JoinType.LEFT);
            root.fetch("usuarioAsignado", JoinType.LEFT);
            query.distinct(true);

            Specification<Tarea> specification = Specification.where(null);

            if (estado != null) {
                specification = specification.and((currentRoot, currentQuery, cb) -> cb.equal(currentRoot.get("estado"), estado));
            }

            if (proyectoId != null) {
                specification = specification.and((currentRoot, currentQuery, cb) ->
                    cb.equal(currentRoot.get("proyecto").get("id"), proyectoId)
                );
            }

            if (usuarioId != null) {
                specification = specification.and((currentRoot, currentQuery, cb) ->
                    cb.equal(currentRoot.get("usuarioAsignado").get("id"), usuarioId)
                );
            }

            if (q != null && !q.isBlank()) {
                String term = "%" + q.trim().toLowerCase() + "%";
                specification = specification.and((currentRoot, currentQuery, cb) -> cb.or(
                    cb.like(cb.lower(currentRoot.get("titulo")), term),
                    cb.like(cb.lower(cb.coalesce(currentRoot.get("descripcion"), "")), term),
                    cb.like(cb.lower(currentRoot.get("proyecto").get("nombre")), term),
                    cb.like(cb.lower(currentRoot.get("usuarioAsignado").get("nombres")), term)
                ));
            }

            return specification.toPredicate(root, query, criteriaBuilder);
        };
    }
}
