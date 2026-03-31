package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.validator.model.enums.Continent;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public abstract class Report {
    private final UUID validatorId;

    private final String platformUrl;

    private final Instant timestamp;

    private final ReportType reportType;

    private final Continent continent;

    public Report(UUID validatorId, String platformUrl, Instant timestamp, ReportType reportType, Continent continent) {
        this.validatorId = validatorId;
        this.timestamp = timestamp;
        this.platformUrl = platformUrl;
        this.reportType = reportType;
        this.continent = continent;
    }
}
