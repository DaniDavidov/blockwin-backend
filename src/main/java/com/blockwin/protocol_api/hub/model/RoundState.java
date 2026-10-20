package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.hub.ReportType;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public class RoundState implements Delayed {
    private final UUID platformId;
    private final String platformURL;
    private final long checkIntervalSeconds;
    private final Instant registrationTimestamp;
    private long roundId;
    private long lastRoundId;
    // Fixed slot [startTimestamp, expiration), set at construction — never derived from report arrival.
    private final Instant startTimestamp;
    private final Instant expiration;
    private final ConcurrentHashMap<UUID, ReportBitmap> bitmapByValidator = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ReportType, List<Report>> reportsByType = new ConcurrentHashMap<>();

    /** First round of a validation period: its slot starts at the validation start. */
    public RoundState(UUID platformId, String platformURL, long checkIntervalSeconds, Instant registrationTimestamp, long lastRoundId) {
        this(platformId, platformURL, checkIntervalSeconds, registrationTimestamp, lastRoundId, 0, registrationTimestamp);
    }

    /**
     * A round whose slot starts at {@code startTimestamp} — the previous round's expiration when
     * resetting, or the current slot's start when recovering after a restart.
     */
    public RoundState(UUID platformId, String platformURL, long checkIntervalSeconds, Instant registrationTimestamp, long lastRoundId, long roundId, Instant startTimestamp) {
        this.reportsByType.put(ReportType.UPTIME, new ArrayList<>());
        this.platformId = platformId;
        this.platformURL = platformURL;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.registrationTimestamp = registrationTimestamp;
        this.roundId = roundId;
        this.lastRoundId = lastRoundId;
        this.startTimestamp = startTimestamp;
        this.expiration = startTimestamp.plusSeconds(checkIntervalSeconds);
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
