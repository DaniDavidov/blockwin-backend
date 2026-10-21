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
    void toEpochId_shouldReturnYYYYMMDDHHmm() {
        assertEquals(202604151200L, EpochService.toEpochId(Instant.parse("2026-04-15T12:00:00Z")));
        assertEquals(202612010000L, EpochService.toEpochId(Instant.parse("2026-12-01T00:00:00Z")));
        assertEquals(202601312359L, EpochService.toEpochId(Instant.parse("2026-01-31T23:59:59Z")));
    }

    @Test
    void toEpochId_shortPeriodsWithinSameDay_areDistinct() {
        long first  = EpochService.toEpochId(Instant.parse("2026-04-01T10:15:00Z"));
        long second = EpochService.toEpochId(Instant.parse("2026-04-01T10:16:00Z"));
        long third  = EpochService.toEpochId(Instant.parse("2026-04-01T22:45:30Z"));

        assertEquals(202604011015L, first);
        assertEquals(202604011016L, second);
        assertEquals(202604012245L, third);
    }

    @Test
    void toEpochId_boundaryAtMonthStart_isCorrect() {
        assertEquals(202604010000L, EpochService.toEpochId(Instant.parse("2026-04-01T00:00:00Z")));
    }

    @Test
    void getCurrentEpochId_shouldBeValidYYYYMMDDHHmmFormat() {
        long id = EpochService.getCurrentEpochId();
        long year   = id / 100_000_000L;
        long month  = (id / 1_000_000L) % 100L;
        long day    = (id / 10_000L) % 100L;
        long hour   = (id / 100L) % 100L;
        long minute = id % 100L;
        assertTrue(year >= 2024 && year <= 2099, "Year out of expected range: " + year);
        assertTrue(month >= 1 && month <= 12,    "Month out of range: " + month);
        assertTrue(day >= 1 && day <= 31,        "Day out of range: " + day);
        assertTrue(hour >= 0 && hour <= 23,      "Hour out of range: " + hour);
        assertTrue(minute >= 0 && minute <= 59,  "Minute out of range: " + minute);
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
        long epochId = 202604010000L;
        Map<EpochParticipationId, Long> increments = new HashMap<>();
        increments.put(new EpochParticipationId(v1, "example.com", epochId), 1L);
        increments.put(new EpochParticipationId(v2, "example.com", epochId), 3L);

        epochService.recordParticipationBatch(increments);

        ArgumentCaptor<SqlParameterSource[]> captor = ArgumentCaptor.forClass(SqlParameterSource[].class);
        verify(jdbc).batchUpdate(contains("INSERT INTO epoch_participation"), captor.capture());
        assertEquals(2, captor.getValue().length);
    }
}
