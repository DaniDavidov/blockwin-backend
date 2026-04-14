package com.blockwin.protocol_api.reward.service;

import com.blockwin.protocol_api.reward.model.EpochParticipationEntity;
import com.blockwin.protocol_api.reward.model.EpochParticipationId;
import com.blockwin.protocol_api.reward.repository.EpochParticipationRepository;
import com.blockwin.protocol_api.reward.repository.PlatformEpochRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpochServiceTest {

    @Mock
    private EpochParticipationRepository repository;

    @Mock
    private PlatformEpochRepository platformEpochRepository;

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
        // Four consecutive weekly periods in April 2026 must all get different epoch IDs
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
        // Midnight UTC on 1 April 2026 - first instant of April
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
    void recordParticipation_newRecord_savesEntityWithCountOne() {
        UUID validatorId = UUID.randomUUID();
        String platformUrl = "example.com";
        long epochId = 20260401L;
        EpochParticipationId id = new EpochParticipationId(validatorId, platformUrl, epochId);

        when(repository.findById(id)).thenReturn(Optional.empty());

        epochService.recordParticipation(validatorId, platformUrl, epochId);

        ArgumentCaptor<EpochParticipationEntity> captor =
                ArgumentCaptor.forClass(EpochParticipationEntity.class);
        verify(repository).save(captor.capture());
        assertEquals(id, captor.getValue().getId());
        assertEquals(1L, captor.getValue().getRoundsParticipated());
    }

    @Test
    void recordParticipation_existingRecord_incrementsRoundsParticipated() {
        UUID validatorId = UUID.randomUUID();
        String platformUrl = "example.com";
        long epochId = 20260401L;
        EpochParticipationId id = new EpochParticipationId(validatorId, platformUrl, epochId);

        EpochParticipationEntity existing = EpochParticipationEntity.builder()
                .id(id)
                .roundsParticipated(5L)
                .build();
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        epochService.recordParticipation(validatorId, platformUrl, epochId);

        verify(repository).save(existing);
        assertEquals(6L, existing.getRoundsParticipated());
    }

    @Test
    void recordParticipation_calledTwice_counterReachesTwo() {
        UUID validatorId = UUID.randomUUID();
        String platformUrl = "example.com";
        long epochId = 20260401L;
        EpochParticipationId id = new EpochParticipationId(validatorId, platformUrl, epochId);

        // First call: no existing record
        when(repository.findById(id)).thenReturn(Optional.empty());
        epochService.recordParticipation(validatorId, platformUrl, epochId);

        ArgumentCaptor<EpochParticipationEntity> captor =
                ArgumentCaptor.forClass(EpochParticipationEntity.class);
        verify(repository).save(captor.capture());
        EpochParticipationEntity saved = captor.getValue();

        // Second call: the saved entity now exists
        reset(repository);
        when(repository.findById(id)).thenReturn(Optional.of(saved));
        epochService.recordParticipation(validatorId, platformUrl, epochId);

        verify(repository).save(saved);
        assertEquals(2L, saved.getRoundsParticipated());
    }
}