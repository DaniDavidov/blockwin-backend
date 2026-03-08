package com.blockwin.protocol_api.hub.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public abstract class Report {
    private final UUID validatorId;

    private final String platformUrl;

    private final Instant timestamp;

    public Report(UUID validatorId, String platformUrl, Instant timestamp) {
        this.validatorId = validatorId;
        this.timestamp = timestamp;
        this.platformUrl = platformUrl;
    }
}
