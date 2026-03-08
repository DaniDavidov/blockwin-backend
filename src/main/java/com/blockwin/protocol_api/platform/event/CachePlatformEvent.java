package com.blockwin.protocol_api.platform.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

@Getter
public class CachePlatformEvent extends ApplicationEvent {
    private final String platformURL;
    private final long checkIntervalSeconds;
    private final Instant registrationTimestamp;
    public CachePlatformEvent(Object source, String platformURL, long checkIntervalSeconds, Instant registrationTimestamp) {
        super(source);
        this.platformURL = platformURL;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.registrationTimestamp = registrationTimestamp;
    }

}
