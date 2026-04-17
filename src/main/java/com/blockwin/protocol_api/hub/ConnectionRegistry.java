package com.blockwin.protocol_api.hub;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionRegistry {
    private final ConcurrentHashMap<UUID, WebSocketSession> connections = new ConcurrentHashMap<>();

    public boolean registerIfAbsent(UUID id, WebSocketSession session) {
        return connections.putIfAbsent(id, session) == null;
    }

    public void unregister(UUID id) {
        connections.remove(id);
    }

    public WebSocketSession getSession(UUID id) {
        return connections.get(id);
    }

    public Collection<WebSocketSession> getAllSessions() {
        return connections.values();
    }
}
