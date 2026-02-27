package com.blockwin.protocol_api.hub.messageHandlers;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class ReportHandler implements MessageHandler {

    @Override
    public void handleMessage(WebSocketSession session, TextMessage message) {

    }
}
