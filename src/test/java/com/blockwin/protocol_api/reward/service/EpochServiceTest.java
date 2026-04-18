package com.blockwin.protocol_api.reward.service;

import com.blockwin.protocol_api.reward.model.EpochParticipationId;
import com.blockwin.protocol_api.reward.repository.PlatformEpochRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpochServiceTest {

    @Mock
    private PlatformEpochRepository platformEpochRepository;

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @InjectMocks
    private EpochService epochService;

    @Test
    void toEpochId_shouldReturnYYYYMMDD() {
        assertEquals(20260415L, EpochService.toEpochId(Instant.parse("2026-04-15T12:00:00Z")));
        assertEquals(20261201L, EpochService.toEpochId(Instant.parse("2026-12-01T00:00:00Z")));
        assertEquals(20260131L, EpochService.toEpochId(Instant.parse("2026-01-31T23:59:59Z")));
    }

    @Test
    void toEpochId_weeklyPeriodsWithinSameMonth_areDistinct() {
        long week1 = EpochService.toEpochId(Instant.parse("2026-04-01T00:00:00Z"));
        long week2 = EpochService.toEpochId(Instant.parse("2026-04-08T00:00:00Z"));
        long week3 = EpochService.toEpochId(Instant.parse("2026-04-15T00:00:00Z"));
        long week4 = EpochService.toEpochId(Instant.parse("2026-04-22T00:00:00Z"));

        assertEquals(20260401L, week1);
        assertEquals(20260408L, week2);
        assertEquals(20260415L, week3);
        assertEquals(20260422L, week4);
    }

    @Test
    void toEpochId_boundaryAtMonthStart_isCorrect() {
        assertEquals(20260401L, EpochService.toEpochId(Instant.parse("2026-04-01T00:00:00Z")));
    }

    @Test
    void getCurrentEpochId_shouldBeValidYYYYMMDDFormat() {
        long id = EpochService.getCurrentEpochId();
        long year  = id / 10_000L;
        long month = (id / 100L) % 100L;
        long day   = id % 100L;
        assertTrue(year >= 2024 && year <= 2099, "Year out of expected range: " + year);
        assertTrue(month >= 1 && month <= 12,    "Month out of range: " + month);
        assertTrue(day >= 1 && day <= 31,        "Day out of range: " + day);
    }

    @Test
    void recordParticipationBatch_emptyMap_skipsJdbcCall() {
        epochService.recordParticipationBatch(Map.of());
        verifyNoInteractions(jdbc);
    }

    @Test
    void recordParticipationBatch_issuesSingleBatchUpdateWithOneParamPerKey() {
        UUID v1 = UUID.randomUUID();
        UUID v2 = UUID.randomUUID();
        long epochId = 20260401L;
        Map<EpochParticipationId, Long> increments = new HashMap<>();
        increments.put(new EpochParticipationId(v1, "example.com", epochId), 1L);
        increments.put(new EpochParticipationId(v2, "example.com", epochId), 3L);

        epochService.recordParticipationBatch(increments);

        ArgumentCaptor<SqlParameterSource[]> captor = ArgumentCaptor.forClass(SqlParameterSource[].class);
        verify(jdbc).batchUpdate(contains("INSERT INTO epoch_participation"), captor.capture());
        assertEquals(2, captor.getValue().length);
    }
}
