package com.blockwin.protocol_api.platform.model.dto;

import java.time.Instant;
import java.util.UUID;

public record PlatformDTO(UUID id, String url, Long checkIntervalSeconds, Instant createdAt) {
}
