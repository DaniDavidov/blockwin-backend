package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.platform.model.dto.PlatformDTO;
import com.blockwin.protocol_api.platform.service.PlatformService;
import com.blockwin.protocol_api.validator.model.enums.ValidatorStatus;
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
    private final ValidatorService validatorService;
    private final ConnectionRegistry connectionRegistry;
    private final MessageDispatcher messageDispatcher;
    private final PlatformService platformService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String validatorId = (String) session.getAttributes().get("validatorId");
        validatorService.setValidatorStatus(UUID.fromString(validatorId), ValidatorStatus.ACTIVE);
        connectionRegistry.register(UUID.fromString(validatorId), session);
        List<PlatformDTO> platforms = platformService.getAllPlatforms();
        try {
            String json = objectMapper.writeValueAsString(platforms);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Error while sending platforms for validation: {}", e.getMessage());
            session.close();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String validatorId = (String) session.getAttributes().get("validatorId");
        validatorService.setValidatorStatus(UUID.fromString(validatorId), ValidatorStatus.INACTIVE);
        connectionRegistry.unregister(UUID.fromString(validatorId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        messageDispatcher.dispatch(session, message);
    }

}
