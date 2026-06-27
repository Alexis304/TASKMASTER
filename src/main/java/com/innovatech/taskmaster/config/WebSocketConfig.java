package com.innovatech.taskmaster.config;

import com.innovatech.taskmaster.websocket.TaskRealtimeWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TaskRealtimeWebSocketHandler taskRealtimeWebSocketHandler;

    public WebSocketConfig(TaskRealtimeWebSocketHandler taskRealtimeWebSocketHandler) {
        this.taskRealtimeWebSocketHandler = taskRealtimeWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(taskRealtimeWebSocketHandler, "/realtime/tasks");
    }
}
