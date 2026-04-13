package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.platform.event.CachePlatformEvent;
import com.blockwin.protocol_api.platform.event.PlatformUpdateEvent;
import com.blockwin.protocol_api.platform.model.dto.PlatformDTO;
import com.blockwin.protocol_api.platform.model.dto.PlatformExpiredNotification;
import com.blockwin.protocol_api.platform.service.PlatformService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Slf4j
@AllArgsConstructor
@Service
public class BroadcastService {

    private final ConnectionRegistry connectionRegistry;
    private final ObjectMapper objectMapper;

    @Order(2)
    @EventListener
    public void onPlatformRegistered(CachePlatformEvent event) {
        if (!(event.getSource() instanceof PlatformService)) {
            return;
        }
        broadcast(new PlatformDTO(event.getPlatformId(), event.getPlatformURL(), event.getCheckIntervalSeconds(), event.getValidationStartTimestamp()));
    }

    @Order(2)
    @EventListener
    public void onPlatformUpdated(PlatformUpdateEvent event) {
        if (!(event.getSource() instanceof PlatformService)) {
            return;
        }
        broadcast(new PlatformDTO(event.getPlatformId(), event.getPlatformURL(), event.getCheckIntervalSeconds(), event.getRegistrationTimestamp()));
    }

    /**
     * Notifies all connected validators that a platform's validation period has ended.
     * Validators should stop submitting reports for the given URL upon receiving this.
     */
    public void broadcastPlatformExpired(String platformUrl) {
        broadcast(new PlatformExpiredNotification(platformUrl));
    }

    private void broadcast(Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize broadcast payload: {}", e.getMessage());
            return;
        }
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : connectionRegistry.getAllSessions()) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.error("Failed to broadcast to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}