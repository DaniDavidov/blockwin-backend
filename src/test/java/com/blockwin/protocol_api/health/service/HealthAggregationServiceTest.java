package com.blockwin.protocol_api.health.service;

import com.blockwin.protocol_api.consensus.model.uptime.ReportCategory;
import com.blockwin.protocol_api.consensus.model.uptime.UptimeConsensusResult;
import com.blockwin.protocol_api.health.model.RoundHealthEntity;
import com.blockwin.protocol_api.health.model.RoundId;
import com.blockwin.protocol_api.health.repository.RoundHealthRepository;
import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.hub.model.ExecutedRound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthAggregationServiceTest {

    private static final String PLATFORM_URL = "http://localhost:4002";

    @Mock private RoundHealthRepository roundHealthRepository;
    @Mock private UptimeHealthService uptimeHealthService;
    @Mock private Acknowledgment ack;

    @InjectMocks private HealthAggregationService healthAggregationService;

    @Test
    void handle_newRound_delegatesToHealthServiceWithoutOverwritingRoundHealth() {
        UptimeConsensusResult result = uptimeResult();
        ExecutedRound round = executedRound(5, result);
        when(roundHealthRepository.findById(new RoundId(5, PLATFORM_URL))).thenReturn(Optional.empty());

        healthAggregationService.handle(round, ack);

        verify(uptimeHealthService).accountHealth(5, PLATFORM_URL, result);
        // RoundHealthEntity is written by UptimeHealthService only; a second save here would overwrite it.
        verify(roundHealthRepository, never()).save(any());
        verify(ack).acknowledge();
    }

    @Test
    void handle_alreadyAccountedRound_isSkipped() {
        ExecutedRound round = executedRound(5, uptimeResult());
        when(roundHealthRepository.findById(new RoundId(5, PLATFORM_URL)))
                .thenReturn(Optional.of(new RoundHealthEntity()));

        healthAggregationService.handle(round, ack);

        verify(uptimeHealthService, never()).accountHealth(anyLong(), anyString(), any());
        verify(roundHealthRepository, never()).save(any());
    }

    private ExecutedRound executedRound(long roundId, UptimeConsensusResult result) {
        Instant start = Instant.parse("2026-10-20T12:00:00Z");
        return new ExecutedRound(roundId, PLATFORM_URL, 202610201230L, start, start.plusSeconds(30), List.of(result));
    }

    private UptimeConsensusResult uptimeResult() {
        return new UptimeConsensusResult(
                ReportType.UPTIME,
                ReportCategory.UNDEFINED,
                0,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                List.of()
        );
    }
}