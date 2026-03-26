package com.blockwin.protocol_api.consensus.model.uptime;

public record Latency(
        long dnsMedian,
        long tcpMedian,
        long tlsMedian,
        long ttfbMedian,
        long totalMedian
) {
}
