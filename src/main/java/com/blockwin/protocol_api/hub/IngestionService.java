package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.hub.model.MessageEnvelope;
import com.blockwin.protocol_api.validator.model.enums.Continent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IngestionService {
    private static final long RATE_LIMIT_WINDOW_MS = 30_000;
    private static final long MAX_MESSAGES_PER_WINDOW = 1_000;

    private final BlockingQueue<MessageEnvelope> queue = new LinkedBlockingQueue<>(100_000);
    private final ObjectMapper mapper = new ObjectMapper();
    // long[0] = window start ms, long[1] = message count in current window
    private final ConcurrentHashMap<UUID, long[]> rateLimitWindows = new ConcurrentHashMap<>();

    public void enqueueMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(message.getPayload());
        if (jsonNode == null || !jsonNode.isObject()) {
            throw new IllegalArgumentException("Invalid message payload");
        }

        JsonNode reportTypeNode = jsonNode.get("report-type");
        if (reportTypeNode == null || reportTypeNode.isNull()) {
            throw new IllegalArgumentException("Missing required field: report-type");
        }

        ReportType reportType;
        try {
            reportType = ReportType.valueOf(reportTypeNode.asText());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown report type: " + reportTypeNode.asText());
        }
        UUID validatorId = (UUID) session.getAttributes().get("validatorId");
        Continent continent = (Continent) session.getAttributes().get("continent");

        checkRateLimit(validatorId);

        MessageEnvelope messageEnvelope = new MessageEnvelope(validatorId, reportType, continent, message.getPayload());
        boolean success = queue.offer(messageEnvelope);
        if (!success) {
            throw new RuntimeException("Unsuccessful enqueue message");
        }
    }

    public MessageEnvelope dequeueMessage() throws InterruptedException {
        return queue.take();
    }

    private void checkRateLimit(UUID validatorId) {
        AtomicBoolean exceeded = new AtomicBoolean(false); // Using this because the lambda function requires any variable it captures from the enclosing scope to be effectively final.
        rateLimitWindows.compute(validatorId, (id, window) -> { // Atomically check and update state
            long now = System.currentTimeMillis();
            if (window == null || now - window[0] > RATE_LIMIT_WINDOW_MS) {
                return new long[]{now, 1};
            }
            if (window[1] >= MAX_MESSAGES_PER_WINDOW) {
                exceeded.set(true);
                return window;
            }
            window[1]++;
            return window;
        });
        if (exceeded.get()) {
            throw new IllegalStateException("rate limit exceeded");
        }
    }
}
