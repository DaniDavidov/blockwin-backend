package com.blockwin.protocol_api.hub.messageHandlers;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public interface MessageHandler {
    public void handleMessage(WebSocketSession session, TextMessage message);
}
