package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.validator.model.enums.ValidatorStatus;
import com.blockwin.protocol_api.validator.service.ValidatorService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

@AllArgsConstructor
@Component
public class ValidatorWebSocketHandler extends TextWebSocketHandler {
    private final ValidatorService validatorService;
    private final ConnectionRegistry connectionRegistry;
    private final MessageDispatcher messageDispatcher;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String validatorId = (String) session.getAttributes().get("validatorId");
        validatorService.setValidatorStatus(UUID.fromString(validatorId), ValidatorStatus.ACTIVE);
        connectionRegistry.register(UUID.fromString(validatorId), session);
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
