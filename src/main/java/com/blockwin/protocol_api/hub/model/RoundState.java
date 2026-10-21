package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.hub.ReportType;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
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
    // Assigned once at deposit time and carried through every round, so participation and the
    // epoch close always agree on the ID regardless of when a round runs.
    private final long epochId;
    // Fixed slot [startTimestamp, expiration), set at construction — never derived from report arrival.
    private final Instant startTimestamp;
    private final Instant expiration;
    private final ConcurrentHashMap<UUID, ReportBitmap> bitmapByValidator = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ReportType, List<Report>> reportsByType = new ConcurrentHashMap<>();

    /** First round of a validation period: its slot starts at the validation start. */
    public RoundState(UUID platformId, String platformURL, long checkIntervalSeconds, Instant registrationTimestamp, long lastRoundId, long epochId) {
        this(platformId, platformURL, checkIntervalSeconds, registrationTimestamp, lastRoundId, epochId, 0, registrationTimestamp);
    }

    /**
     * A round whose slot starts at {@code startTimestamp} — the previous round's expiration when
     * resetting, or the current slot's start when recovering after a restart.
     */
    public RoundState(UUID platformId, String platformURL, long checkIntervalSeconds, Instant registrationTimestamp, long lastRoundId, long epochId, long roundId, Instant startTimestamp) {
        this.reportsByType.put(ReportType.UPTIME, new ArrayList<>());
        this.platformId = platformId;
        this.platformURL = platformURL;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.registrationTimestamp = registrationTimestamp;
        this.roundId = roundId;
        this.lastRoundId = lastRoundId;
        this.epochId = epochId;
        this.startTimestamp = startTimestamp;
        this.expiration = startTimestamp.plusSeconds(checkIntervalSeconds);
    }

    /**
     * Rounds fill {@code [validationStart, validationEnd)} back-to-back, so round {@code lastRoundId}
     * expires at {@code validationEnd} (or earlier, if the period is not a whole number of intervals).
     */
    public static long lastRoundId(Instant validationStart, Instant validationEnd, long checkIntervalSeconds) {
        long rounds = Duration.between(validationStart, validationEnd).getSeconds() / checkIntervalSeconds;
        return Math.max(0, rounds - 1);
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
