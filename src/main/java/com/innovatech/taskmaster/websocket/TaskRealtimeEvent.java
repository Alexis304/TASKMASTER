package com.innovatech.taskmaster.websocket;

import com.innovatech.taskmaster.dto.TareaResponse;

public record TaskRealtimeEvent(
    String type,
    String message,
    TareaResponse tarea,
    Long deletedTaskId
) {
}
