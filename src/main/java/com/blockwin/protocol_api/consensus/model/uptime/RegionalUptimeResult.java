package com.blockwin.protocol_api.consensus.model.uptime;

import com.blockwin.protocol_api.validator.model.enums.Continent;

import java.util.Map;
import java.util.UUID;

public record RegionalUptimeResult(
        Continent continent,
        ReportCategory votedCategory,
        long agreement,
        long dnsMedian,
        long tcpMedian,
        long tlsMedian,
        long ttfbMedian,
        long totalMedian,
        Map<UUID, Boolean> validatorCorrectness
) {
}
