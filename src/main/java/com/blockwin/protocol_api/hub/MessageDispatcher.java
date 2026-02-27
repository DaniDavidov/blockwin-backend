package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.hub.messageHandlers.MessageHandler;
import com.blockwin.protocol_api.hub.messageHandlers.ReportHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageDispatcher {
    private final ConcurrentHashMap<MessageType, MessageHandler> messageHandlers = new ConcurrentHashMap<>();

    @Autowired
    public MessageDispatcher(ReportHandler reportHandler) {
        messageHandlers.put(MessageType.REPORT, reportHandler);
    }

    public void dispatch(WebSocketSession session, TextMessage message) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(message.getPayload());
        String type = jsonNode.get("type").asText();
        MessageType messageType = MessageType.valueOf(type);
        MessageHandler messageHandler = messageHandlers.get(messageType);
        if (messageHandler == null) {
            throw new RuntimeException("Unknown message type: " + type);
        }
        messageHandler.handleMessage(session, message);
    }

}
