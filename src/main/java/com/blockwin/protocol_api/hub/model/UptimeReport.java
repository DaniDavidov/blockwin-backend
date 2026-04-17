package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.health.model.Status;
import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.validator.model.enums.Continent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class UptimeReport extends Report {

    private final Status status;
    private final long dnsResolutionTime;
    private final long tcpConnectTime;
    private final long tlsHandshakeTime;
    private final long timeToFirstByte;
    private final long totalResponseTime;

    @JsonCreator
    public UptimeReport(@JsonProperty("platformUrl") String platformUrl,
                        @JsonProperty("timestamp") Instant timestamp,
                        @JsonProperty("status") Status status,
                        @JsonProperty("dnsResolutionTime") long dnsResolutionTime,
                        @JsonProperty("tcpConnectTime") long tcpConnectTime,
                        @JsonProperty("tlsHandshakeTime") long tlsHandshakeTime,
                        @JsonProperty("timeToFirstByte") long timeToFirstByte,
                        @JsonProperty("totalResponseTime") long totalResponseTime
    ) {
        super(platformUrl, timestamp);
        this.status = status;
        this.dnsResolutionTime = dnsResolutionTime;
        this.tcpConnectTime = tcpConnectTime;
        this.tlsHandshakeTime = tlsHandshakeTime;
        this.timeToFirstByte = timeToFirstByte;
        this.totalResponseTime = totalResponseTime;
    }
}
