package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.validator.model.enums.Continent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Data
@JsonIgnoreProperties(value = {"validatorId", "continent", "reportType"}, ignoreUnknown = true)
public abstract class Report {
    @Setter
    private UUID validatorId;

    private final String platformUrl;

    private final Instant timestamp;

    @Setter
    private ReportType reportType;

    @Setter
    private Continent continent;

    public Report(String platformUrl, Instant timestamp) {
        this.timestamp = timestamp;
        this.platformUrl = platformUrl;
    }
}
