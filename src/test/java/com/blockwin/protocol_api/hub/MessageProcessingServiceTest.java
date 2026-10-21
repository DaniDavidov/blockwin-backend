package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.hub.model.RoundState;
import com.blockwin.protocol_api.hub.processors.UptimeReportProcessor;
import com.blockwin.protocol_api.platform.event.CachePlatformEvent;
import com.blockwin.protocol_api.reward.service.RewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MessageProcessingServiceTest {

    @Mock private StateRegistry stateRegistry;
    @Mock private StateUpdateRegistry stateUpdateRegistry;
    @Mock private IngestionService ingestionService;
    @Mock private UptimeReportProcessor uptimeReportProcessor;
    @Mock private RewardService rewardService;

    private MessageProcessingService messageProcessingService;

    @BeforeEach
    void setUp() {
        // Construct manually so @PostConstruct worker threads are not started.
        messageProcessingService = new MessageProcessingService(
                ingestionService, uptimeReportProcessor, stateRegistry, stateUpdateRegistry);
    }

    @Test
    void cachePlatform_fromRewardService_registersStateWithCorrectFields() {
        UUID platformId = UUID.randomUUID();
        Instant start = Instant.parse("2026-04-13T00:00:00Z");
        Instant end = start.plus(30, ChronoUnit.DAYS);
        long intervalSeconds = 30L;
        long epochId = 202605130000L;
        long expectedLastRoundId = (end.getEpochSecond() - start.getEpochSecond()) / intervalSeconds - 1;

        CachePlatformEvent event = new CachePlatformEvent(
                rewardService, platformId, "example.com", epochId, intervalSeconds, start, end);

        messageProcessingService.cachePlatform(event);

        ArgumentCaptor<RoundState> captor = ArgumentCaptor.forClass(RoundState.class);
        verify(stateRegistry).registerState(captor.capture());
        RoundState state = captor.getValue();
        assertEquals(platformId, state.getPlatformId());
        assertEquals("example.com", state.getPlatformURL());
        assertEquals(intervalSeconds, state.getCheckIntervalSeconds());
        assertEquals(start, state.getRegistrationTimestamp());
        assertEquals(expectedLastRoundId, state.getLastRoundId());
        assertEquals(epochId, state.getEpochId());
        assertEquals(0L, state.getRoundId());
    }

    @Test
    void cachePlatform_lastRoundId_lastWholeRoundEndsAtValidationEnd() {
        // 10-minute validation period with a 30-second interval: 20 rounds, IDs 0..19,
        // so round 19 expires exactly at the end of the period.
        Instant start = Instant.parse("2026-04-13T00:00:00Z");
        Instant end = start.plus(10, ChronoUnit.MINUTES);
        long intervalSeconds = 30L;
        long expectedLastRoundId = 19L;

        CachePlatformEvent event = new CachePlatformEvent(
                rewardService, UUID.randomUUID(), "example.com", 202604130010L, intervalSeconds, start, end);

        messageProcessingService.cachePlatform(event);

        ArgumentCaptor<RoundState> captor = ArgumentCaptor.forClass(RoundState.class);
        verify(stateRegistry).registerState(captor.capture());
        assertEquals(expectedLastRoundId, captor.getValue().getLastRoundId());
    }

    @Test
    void cachePlatform_fromWrongSource_doesNotRegisterState() {
        CachePlatformEvent event = new CachePlatformEvent(
                this, UUID.randomUUID(), "example.com", 202605130000L, 30L,
                Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS));

        messageProcessingService.cachePlatform(event);

        verifyNoInteractions(stateRegistry);
    }
}