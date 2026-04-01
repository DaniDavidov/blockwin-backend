package com.blockwin.protocol_api.health.service;

import com.blockwin.protocol_api.consensus.model.ConsensusResult;
import com.blockwin.protocol_api.consensus.model.uptime.Latency;
import com.blockwin.protocol_api.consensus.model.uptime.ReportCategory;
import com.blockwin.protocol_api.consensus.model.uptime.UptimeConsensusResult;
import com.blockwin.protocol_api.health.model.ReportEntity;
import com.blockwin.protocol_api.health.model.RoundHealthEntity;
import com.blockwin.protocol_api.health.model.RoundId;
import com.blockwin.protocol_api.health.model.UptimeRegionResultEntity;
import com.blockwin.protocol_api.health.repository.ReportRepository;
import com.blockwin.protocol_api.health.repository.RoundHealthRepository;
import com.blockwin.protocol_api.health.repository.UptimeRegionResultRepository;
import com.blockwin.protocol_api.hub.model.UptimeReport;
import com.blockwin.protocol_api.validator.model.enums.Continent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class UptimeHealthService implements HealthService {
    private final ReportRepository reportRepository;
    private final UptimeRegionResultRepository uptimeRegionResultRepository;
    private final RoundHealthRepository roundHealthRepository;

    @Override
    public void accountHealth(long roundId, String platformUrl, ConsensusResult consensusResult) {
        UptimeConsensusResult uptimeConsensusResult = (UptimeConsensusResult) consensusResult;
        RoundHealthEntity roundHealth = RoundHealthEntity.builder()
                .roundId(new RoundId(roundId, platformUrl))
                .votedCategory(uptimeConsensusResult.winnerCategory().toString())
                .certainty(uptimeConsensusResult.certainty())
                .createdAt(Instant.now())
                .build();
        roundHealthRepository.save(roundHealth);

        List<ReportEntity> reports = uptimeConsensusResult
                .reports()
                .stream()
                .map(uptimeReport -> mapToReportEntity(uptimeReport, roundId))
                .toList();
        reportRepository.saveAll(reports);

        List<UptimeRegionResultEntity> regionResults = new ArrayList<>();
        for (Continent continent : Continent.values()) {
            ReportCategory reportCategory = uptimeConsensusResult.categoryByRegion().get(continent);
            Latency latency = uptimeConsensusResult.latencyByRegion().get(continent);
            UptimeRegionResultEntity regionResult = UptimeRegionResultEntity.builder()
                    .category(reportCategory.toString())
                    .continent(continent.toString())
                    .dns(latency.dnsMedian())
                    .tcp(latency.tcpMedian())
                    .tls(latency.tlsMedian())
                    .ttfb(latency.ttfbMedian())
                    .total(latency.totalMedian())
                    .round(roundId)
                    .build();
            regionResults.add(regionResult);
        }
        uptimeRegionResultRepository.saveAll(regionResults);
    }

    private ReportEntity mapToReportEntity(UptimeReport report, long roundId) {
        return ReportEntity.builder()
                .status(report.getStatus())
                .dns(report.getDnsResolutionTime())
                .tcp(report.getTcpConnectTime())
                .tls(report.getTlsHandshakeTime())
                .ttfb(report.getTimeToFirstByte())
                .latency(report.getTotalResponseTime())
                .timestamp(Instant.now())
                .platformUrl(report.getPlatformUrl())
                .validatorId(report.getValidatorId())
                .roundId(roundId)
                .build();
    }
}
