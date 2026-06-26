package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.model.Tarea;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Async
    public void notificarNuevaTarea(Tarea tarea) {
        if (tarea == null) {
            return;
        }

        System.out.println("Notificacion asincrona generada para la tarea: " + tarea.getTitulo());
    }
}
