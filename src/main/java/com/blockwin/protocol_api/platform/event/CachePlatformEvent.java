package com.blockwin.protocol_api.platform.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.UUID;

@Getter
public class CachePlatformEvent extends ApplicationEvent {
    private final UUID platformId;
    private final String platformURL;
    private final long checkIntervalSeconds;
    private final Instant registrationTimestamp;
    private final Instant validationEndTimestamp;

    public CachePlatformEvent(Object source, UUID platformId, String platformURL, long checkIntervalSeconds, Instant registrationTimestamp, Instant validationEndTimestamp) {
        super(source);
        this.platformId = platformId;
        this.platformURL = platformURL;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.registrationTimestamp = registrationTimestamp;
        this.validationEndTimestamp = validationEndTimestamp;
    }
}
