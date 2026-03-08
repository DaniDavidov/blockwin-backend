package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.hub.model.MessageEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class IngestionService {
    private final BlockingQueue<MessageEnvelope> queue = new LinkedBlockingQueue<>(100_000);
    private final ObjectMapper mapper = new ObjectMapper();

    // TODO: implement input sanitization -> currently, arbitrary data is being enqueued
    public void enqueueMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(message.getPayload());
        if (jsonNode == null) {
            throw new RuntimeException("Invalid message payload");
        }
        ReportType reportType = ReportType.valueOf(jsonNode.get("report-type").asText());
        UUID validatorId = (UUID) session.getAttributes().get("validatorId");

        MessageEnvelope messageEnvelope = new MessageEnvelope(validatorId, reportType, message.getPayload());
        boolean success = queue.offer(messageEnvelope);
        if (!success) {
            throw new RuntimeException("Unsuccessful enqueue message");
        }
    }

    public MessageEnvelope dequeueMessage() throws InterruptedException {
        return queue.take();
    }
}
