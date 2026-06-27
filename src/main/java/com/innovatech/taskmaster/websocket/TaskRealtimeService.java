package com.innovatech.taskmaster.websocket;

import com.innovatech.taskmaster.dto.TareaResponse;
import org.springframework.stereotype.Service;

@Service
public class TaskRealtimeService {

    private final TaskRealtimeWebSocketHandler webSocketHandler;

    public TaskRealtimeService(TaskRealtimeWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public void tareaCreada(TareaResponse tarea) {
        webSocketHandler.broadcast(new TaskRealtimeEvent(
            "TASK_CREATED",
            "Nueva tarea creada: " + tarea.titulo(),
            tarea,
            null,
            tarea.proyectoId()
        ));
    }

    public void tareaActualizada(TareaResponse tarea) {
        webSocketHandler.broadcast(new TaskRealtimeEvent(
            "TASK_UPDATED",
            "Tarea actualizada: " + tarea.titulo(),
            tarea,
            null,
            tarea.proyectoId()
        ));
    }

    public void tareaMovida(TareaResponse tarea) {
        webSocketHandler.broadcast(new TaskRealtimeEvent(
            "TASK_MOVED",
            "Tarea movida a " + tarea.estado(),
            tarea,
            null,
            tarea.proyectoId()
        ));
    }

    public void tareaEliminada(Long tareaId, Long proyectoId, String titulo) {
        webSocketHandler.broadcast(new TaskRealtimeEvent(
            "TASK_DELETED",
            "Tarea eliminada: " + titulo,
            null,
            tareaId,
            proyectoId
        ));
    }
}
