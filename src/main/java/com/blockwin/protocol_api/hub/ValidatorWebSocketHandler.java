package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.consensus.cache.ValidatorReputationCacheService;
import com.blockwin.protocol_api.platform.model.dto.PlatformDTO;
import com.blockwin.protocol_api.validator.model.enums.ValidatorStatus;
import com.blockwin.protocol_api.validator.service.ValidatorScoringService;
import com.blockwin.protocol_api.validator.service.ValidatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Component
public class ValidatorWebSocketHandler extends TextWebSocketHandler {
    private static final int MAX_PAYLOAD_BYTES = 2_048;

    private final ValidatorService validatorService;
    private final ConnectionRegistry connectionRegistry;
    private final IngestionService ingestionService;
    private final StateRegistry stateRegistry;
    private final ValidatorReputationCacheService validatorReputationCacheService;
    private final ValidatorScoringService validatorScoringService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        UUID validatorId = (UUID) session.getAttributes().get("validatorId");
        if (!connectionRegistry.registerIfAbsent(validatorId, session)) {
            log.warn("Duplicate session rejected for validator {}", validatorId);
            session.close(new CloseStatus(4000, "duplicate session"));
            return;
        }
        validatorService.setValidatorStatus(validatorId, ValidatorStatus.ACTIVE);
        int validatorReputation = validatorScoringService.getValidatorReputation(validatorId);
        validatorReputationCacheService.cacheValidator(validatorId, validatorReputation);
        List<PlatformDTO> platforms = stateRegistry.getActivePlatforms();
        try {
            String json = objectMapper.writeValueAsString(platforms);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Error while sending platforms for validation: {}", e.getMessage());
            session.close(new CloseStatus(1011, e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID validatorId = (UUID) session.getAttributes().get("validatorId");
        validatorService.setValidatorStatus(validatorId, ValidatorStatus.INACTIVE);
        connectionRegistry.unregister(validatorId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if (message.getPayloadLength() > MAX_PAYLOAD_BYTES) {
            log.warn("Rejected oversized message ({} bytes) from validator {}", message.getPayloadLength(), session.getAttributes().get("validatorId"));
            return;
        }
        try {
            ingestionService.enqueueMessage(session, message);
        } catch (Exception e) {
            log.warn("Rejected message from validator {}: {}", session.getAttributes().get("validatorId"), e.getMessage());
        }
    }

}
