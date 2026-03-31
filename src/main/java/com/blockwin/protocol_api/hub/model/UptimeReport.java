package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.health.model.Status;
import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.validator.model.enums.Continent;
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

    public UptimeReport(UUID validatorId,
                        String platformUrl,
                        Instant timestamp,
                        ReportType reportType,
                        Continent continent,
                        Status status,
                        long dnsResolutionTime,
                        long tcpConnectTime,
                        long tlsHandshakeTime,
                        long timeToFirstByte,
                        long totalResponseTime
    ) {
        super(validatorId, platformUrl,timestamp, reportType, continent);
        this.status = status;
        this.dnsResolutionTime = dnsResolutionTime;
        this.tcpConnectTime = tcpConnectTime;
        this.tlsHandshakeTime = tlsHandshakeTime;
        this.timeToFirstByte = timeToFirstByte;
        this.totalResponseTime = totalResponseTime;
    }
}
