package com.blockwin.protocol_api.health.service;

import com.blockwin.protocol_api.consensus.model.uptime.Latency;
import com.blockwin.protocol_api.consensus.model.uptime.ReportCategory;
import com.blockwin.protocol_api.consensus.model.uptime.UptimeConsensusResult;
import com.blockwin.protocol_api.health.model.Status;
import com.blockwin.protocol_api.health.model.UptimeRegionResultEntity;
import com.blockwin.protocol_api.health.repository.ReportRepository;
import com.blockwin.protocol_api.health.repository.RoundHealthRepository;
import com.blockwin.protocol_api.health.repository.UptimeRegionResultRepository;
import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.hub.model.UptimeReport;
import com.blockwin.protocol_api.validator.model.enums.Continent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UptimeHealthServiceTest {

    private static final String PLATFORM_URL = "http://localhost:4002";

    @Mock private ReportRepository reportRepository;
    @Mock private UptimeRegionResultRepository uptimeRegionResultRepository;
    @Mock private RoundHealthRepository roundHealthRepository;

    @InjectMocks private UptimeHealthService uptimeHealthService;

    @Test
    void accountHealth_singleReportingContinent_persistsOnlyThatRegion() {
        UUID validatorId = UUID.randomUUID();
        UptimeReport report = new UptimeReport(PLATFORM_URL, Instant.now(), Status.OK, 10, 20, 30, 40, 100);
        report.setValidatorId(validatorId);
        report.setContinent(Continent.EUROPE);
        report.setReportType(ReportType.UPTIME);

        UptimeConsensusResult result = new UptimeConsensusResult(
                ReportType.UPTIME,
                ReportCategory.HEALTHY,
                10_000,
                Map.of(Continent.EUROPE, ReportCategory.HEALTHY),
                Map.of(Continent.EUROPE, new Latency(10, 20, 30, 40, 100)),
                Map.of(validatorId, true),
                List.of(report)
        );

        uptimeHealthService.accountHealth(3, PLATFORM_URL, result);

        List<UptimeRegionResultEntity> saved = captureRegionResults();
        assertEquals(1, saved.size());
        UptimeRegionResultEntity region = saved.get(0);
        assertEquals(Continent.EUROPE.toString(), region.getContinent());
        assertEquals(ReportCategory.HEALTHY.toString(), region.getCategory());
        assertEquals(10, region.getDns());
        assertEquals(20, region.getTcp());
        assertEquals(30, region.getTls());
        assertEquals(40, region.getTtfb());
        assertEquals(100, region.getTotal());
        assertEquals(3, region.getRound());
    }

    @Test
    void accountHealth_noRegionalResults_persistsNoRegionsWithoutThrowing() {
        // Shape produced by UptimeConsensusMechanism when no region has a voted category.
        UptimeConsensusResult result = new UptimeConsensusResult(
                ReportType.UPTIME,
                ReportCategory.UNDEFINED,
                0,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                List.of()
        );

        uptimeHealthService.accountHealth(0, PLATFORM_URL, result);

        assertTrue(captureRegionResults().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private List<UptimeRegionResultEntity> captureRegionResults() {
        ArgumentCaptor<List<UptimeRegionResultEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(uptimeRegionResultRepository).saveAll(captor.capture());
        return captor.getValue();
    }
}