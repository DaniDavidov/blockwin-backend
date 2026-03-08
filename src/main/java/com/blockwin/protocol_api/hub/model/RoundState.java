package com.blockwin.protocol_api.hub.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public class RoundState implements Delayed {
    private final String platformURL;
    private final long checkIntervalSeconds;
    private final Instant registrationTimestamp;
    private long roundId;
    private boolean finalized;
    private Instant expiration;
    private final HashMap<UUID, Report> platformReports = new HashMap<>();

    public RoundState(String platformURL, long checkIntervalSeconds, Instant registrationTimestamp) {
        this.platformURL = platformURL;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.registrationTimestamp = registrationTimestamp;
        this.roundId = -1;
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return expiration.toEpochMilli() - Instant.now().toEpochMilli();
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
    }
}
