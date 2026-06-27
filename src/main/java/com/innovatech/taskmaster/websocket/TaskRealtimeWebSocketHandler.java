package com.innovatech.taskmaster.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class TaskRealtimeWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> sessionProjectIds = new ConcurrentHashMap<>();

    public TaskRealtimeWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        parseProjectId(session).ifPresentOrElse(projectId -> {
            sessionProjectIds.put(session.getId(), projectId);
            sessions.add(session);
        }, () -> closeQuietly(session));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        sessionProjectIds.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session);
        sessionProjectIds.remove(session.getId());
    }

    public void broadcast(TaskRealtimeEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (IOException exception) {
            return;
        }

        TextMessage message = new TextMessage(payload);
        sessions.removeIf(session -> {
            if (session.isOpen()) {
                return false;
            }
            sessionProjectIds.remove(session.getId());
            return true;
        });
        sessions.stream()
            .filter(session -> isSubscribedToProject(session, event.proyectoId()))
            .forEach(session -> send(session, message));
    }

    private Optional<Long> parseProjectId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return Optional.empty();
        }

        return Arrays.stream(uri.getQuery().split("&"))
            .map(parameter -> parameter.split("=", 2))
            .filter(parts -> parts.length == 2 && "proyectoId".equals(parts[0]))
            .map(parts -> parseLong(parts[1]))
            .flatMap(Optional::stream)
            .findFirst();
    }

    private Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private boolean isSubscribedToProject(WebSocketSession session, Long projectId) {
        return projectId != null && projectId.equals(sessionProjectIds.get(session.getId()));
    }

    private void send(WebSocketSession session, TextMessage message) {
        try {
            session.sendMessage(message);
        } catch (IOException exception) {
            sessions.remove(session);
            sessionProjectIds.remove(session.getId());
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.BAD_DATA);
        } catch (IOException exception) {
            sessions.remove(session);
        }
    }
}
