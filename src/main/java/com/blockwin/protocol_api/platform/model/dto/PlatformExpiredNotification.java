package com.blockwin.protocol_api.platform.model.dto;

/**
 * Sent over WebSocket to all connected validators when a platform's validation
 * period ends. Validators should stop submitting reports for the given URL.
 */
public record PlatformExpiredNotification(String type, String platformUrl) {

    public PlatformExpiredNotification(String platformUrl) {
        this("PLATFORM_EXPIRED", platformUrl);
    }
}