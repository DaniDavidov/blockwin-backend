package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.hub.ReportType;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public abstract class Report {
    private final UUID validatorId;

    private final String platformUrl;

    private final Instant timestamp;

    private final ReportType reportType;

    public Report(UUID validatorId, String platformUrl, Instant timestamp, ReportType reportType) {
        this.validatorId = validatorId;
        this.timestamp = timestamp;
        this.platformUrl = platformUrl;
        this.reportType = reportType;
    }
}
