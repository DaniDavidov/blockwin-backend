package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.platform.event.CachePlatformEvent;
import com.blockwin.protocol_api.platform.event.PlatformUpdateEvent;
import com.blockwin.protocol_api.platform.model.dto.PlatformDTO;
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
        PlatformDTO dto = new PlatformDTO(event.getPlatformURL(), event.getCheckIntervalSeconds(), event.getRegistrationTimestamp());
        broadcast(dto);
    }

    @Order(2)
    @EventListener
    public void onPlatformUpdated(PlatformUpdateEvent event) {
        if (!(event.getSource() instanceof PlatformService)) {
            return;
        }
        PlatformDTO dto = new PlatformDTO(event.getPlatformURL(), event.getCheckIntervalSeconds(), event.getRegistrationTimestamp());
        broadcast(dto);
    }

    private void broadcast(PlatformDTO dto) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PlatformDTO for broadcast: {}", e.getMessage());
            return;
        }

        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : connectionRegistry.getAllSessions()) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.error("Failed to broadcast platform update to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}