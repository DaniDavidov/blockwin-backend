package com.blockwin.protocol_api.platform.model.dto;

import java.time.Instant;

public record PlatformDTO(String url, Long checkIntervalSeconds, Instant createdAt) {
}
