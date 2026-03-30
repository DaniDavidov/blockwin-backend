package com.blockwin.protocol_api.consensus;

import com.blockwin.protocol_api.consensus.model.uptime.Latency;
import com.blockwin.protocol_api.consensus.model.uptime.RegionalUptimeResult;
import com.blockwin.protocol_api.consensus.model.uptime.ReportCategory;
import com.blockwin.protocol_api.consensus.model.uptime.UptimeConsensusResult;
import com.blockwin.protocol_api.health.model.Status;
import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.hub.model.Report;
import com.blockwin.protocol_api.hub.model.UptimeReport;
import com.blockwin.protocol_api.validator.model.enums.Continent;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.blockwin.protocol_api.common.utils.Constants.*;


@Component
public class UptimeConsensusMechanism implements ConsensusMechanism<UptimeReport, UptimeConsensusResult> {

    @Override
    public ReportType supportedType() {
        return ReportType.UPTIME;
    }

    @Override
    public UptimeConsensusResult execute(List<UptimeReport> reports, Map<UUID, Integer> reputationsByIds) {

        Map<Continent, List<UptimeReport>> regionBuckets = splitByRegion(reports);

        List<RegionalUptimeResult> regionalResults = new ArrayList<>();

        for (Map.Entry<Continent, List<UptimeReport>> entry : regionBuckets.entrySet()) {

            RegionalUptimeResult result =
                    executeRegionalConsensus(entry.getKey(), entry.getValue(), reputationsByIds);

            regionalResults.add(result);
        }

        return aggregateGlobal(regionalResults, reports);
    }

    private Map<Continent, List<UptimeReport>> splitByRegion(List<UptimeReport> reports) {
        Map<Continent, List<UptimeReport>> buckets = new EnumMap<>(Continent.class);
        for (UptimeReport report : reports) {
            buckets
                    .computeIfAbsent(report.getContinent(), r -> new ArrayList<>())
                    .add(report);
        }
        return buckets;
    }

    private RegionalUptimeResult executeRegionalConsensus(
            Continent continent,
            List<UptimeReport> reports,
            Map<UUID, Integer> reputationsByIds) {

        if (reports.size() < MIN_VALIDATORS) {
            return new RegionalUptimeResult(
                    continent,
                    ReportCategory.UNDEFINED,
                    0,
                    -1,-1,-1,-1,-1,
                    Collections.EMPTY_MAP
            );
        }

        Map<ReportCategory, Long> votes = new EnumMap<>(ReportCategory.class);
        List<Long> dns = new ArrayList<>();
        List<Long> tcp = new ArrayList<>();
        List<Long> tls = new ArrayList<>();
        List<Long> ttfb = new ArrayList<>();
        List<Long> total = new ArrayList<>();

        for (UptimeReport r : reports) {
            Integer weighedVote = reputationsByIds.get(r.getValidatorId());
            votes.merge(mapStatus(r.getStatus()), Long.valueOf(weighedVote), Long::sum);

            if (r.getStatus() == Status.OK) {
                dns.add(r.getDnsResolutionTime());
                tcp.add(r.getTcpConnectTime());
                tls.add(r.getTlsHandshakeTime());
                ttfb.add(r.getTimeToFirstByte());
                total.add(r.getTotalResponseTime());
            }
        }

        ReportCategory winner = null;
        long maxVotes = 0;
        long totalVotingPower = 0;

        for (Map.Entry<ReportCategory, Long> e : votes.entrySet()) {
            if (e.getValue() > maxVotes) {
                maxVotes = e.getValue();
                winner = e.getKey();
            }
            totalVotingPower += e.getValue();
        }
        long agreement = MAX_BPS * maxVotes / totalVotingPower;

        long dnsMedian = median(dns);
        long tcpMedian = median(tcp);
        long tlsMedian = median(tls);
        long ttfbMedian = median(ttfb);
        long totalMedian = median(total);

        Map<UUID, Boolean> correctness = new HashMap<>();

        for (UptimeReport r : reports) {
            ReportCategory category = mapStatus(r.getStatus());

            // category mismatch
            if (category != winner) {
                correctness.put(r.getValidatorId(), false);
                continue;
            }

            // latency deviation checks only for OK reports
            if (r.getStatus() == Status.OK) {
                if (
                        isLatencyOutlier(r.getDnsResolutionTime(), dnsMedian) ||
                        isLatencyOutlier(r.getTcpConnectTime(), tcpMedian) ||
                        isLatencyOutlier(r.getTlsHandshakeTime(), tlsMedian) ||
                        isLatencyOutlier(r.getTimeToFirstByte(), ttfbMedian) ||
                        isLatencyOutlier(r.getTotalResponseTime(), totalMedian)) {

                    correctness.put(r.getValidatorId(), false);
                    continue;
                }
            }
            correctness.put(r.getValidatorId(), true);
        }
        return new RegionalUptimeResult(
                continent,
                winner,
                agreement,
                dnsMedian,
                tcpMedian,
                tlsMedian,
                ttfbMedian,
                totalMedian,
                correctness
        );
    }

    private UptimeConsensusResult aggregateGlobal(
            List<RegionalUptimeResult> regionalResults,
            List<UptimeReport> reports
    ) {

        Map<ReportCategory, Integer> globalVotes = new EnumMap<>(ReportCategory.class);
        Map<Continent, Latency> latenciesPerRegion = new EnumMap<>(Continent.class);
        Map<Continent, ReportCategory> winnerCategoryPerRegion= new EnumMap<>(Continent.class);
        Map<UUID, Boolean> validatorCorrectness = new HashMap<>();


        for (RegionalUptimeResult r : regionalResults) {
            if (r.votedCategory() == null) {
                continue;
            }
            globalVotes.merge(r.votedCategory(), 1, Integer::sum);
            Latency latency = new Latency(
                    r.dnsMedian(),
                    r.tcpMedian(),
                    r.tlsMedian(),
                    r.ttfbMedian(),
                    r.totalMedian()
            );
            latenciesPerRegion.put(r.continent(), latency);
            winnerCategoryPerRegion.put(r.continent(), r.votedCategory());
            r.validatorCorrectness().forEach((key, value) -> {
                Boolean b = validatorCorrectness.get(key);
                if (b == null) {
                    validatorCorrectness.put(key, value);
                } else { // sanity check in case when validator changes region and becomes malicious in between report submissions
                    if (!b == value) {
                        validatorCorrectness.put(key, false);
                    }
                }
            });
        }

        ReportCategory globalCategory = null;
        int maxVotes = 0;
        int totalVotes = 0;

        for (Map.Entry<ReportCategory, Integer> e : globalVotes.entrySet()) {

            if (e.getValue() > maxVotes) {
                maxVotes = e.getValue();
                globalCategory = e.getKey();
            }
            totalVotes += e.getValue();
        }

        if (globalVotes.isEmpty()) {
            return new UptimeConsensusResult(
                    ReportType.UPTIME,
                    ReportCategory.UNDEFINED,
                    0,
                    Collections.EMPTY_MAP,
                    Collections.EMPTY_MAP,
                    Collections.EMPTY_MAP,
                    reports
            );
        }

        int certainty = maxVotes * MAX_BPS / totalVotes;

        return new UptimeConsensusResult(
                ReportType.UPTIME,
                globalCategory,
                certainty,
                winnerCategoryPerRegion,
                latenciesPerRegion,
                validatorCorrectness,
                reports
        );
    }

    private boolean isLatencyOutlier(long value, long median) {
        if (median <= 0) {
            return false;
        }

        long lowerBound = median * (MAX_BPS - LATENCY_DEVIATION_FACTOR_BPS) / MAX_BPS;
        long upperBound = median * (MAX_BPS + LATENCY_DEVIATION_FACTOR_BPS) / MAX_BPS;

        return value > upperBound || value < lowerBound;
    }

    private long median(List<Long> values) {
        if (values.isEmpty()) {
            return -1;
        }
        Collections.sort(values);
        int size = values.size();
        int middle = size / 2;

        if (size % 2 == 0) {
            return (values.get(middle - 1) + values.get(middle)) / 2;
        }
        return values.get(middle);
    }

    private ReportCategory mapStatus(Status status) {

        return switch (status) {
            case OK -> ReportCategory.HEALTHY;
            case DNS_FAILURE, TCP_TIMEOUT, CONNECTION_REFUSED, TIMEOUT -> ReportCategory.NETWORK_FAILURE;
            case HTTP_5XX -> ReportCategory.SERVER_FAILURE;
            case HTTP_4XX -> ReportCategory.CLIENT_ERROR;
            case TLS_FAILURE, INVALID_CERT -> ReportCategory.SECURITY_FAILURE;
        };
    }
}