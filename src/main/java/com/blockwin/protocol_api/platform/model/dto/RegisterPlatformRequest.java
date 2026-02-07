package com.blockwin.protocol_api.platform.model.dto;

public record RegisterPlatformRequest(String userEmail, String url, Long checkIntervalSeconds) {
}
