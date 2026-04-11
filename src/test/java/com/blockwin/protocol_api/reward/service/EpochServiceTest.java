package com.blockwin.protocol_api.reward.service;

import com.blockwin.protocol_api.reward.model.EpochParticipationEntity;
import com.blockwin.protocol_api.reward.model.EpochParticipationId;
import com.blockwin.protocol_api.reward.repository.EpochParticipationRepository;
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

    @InjectMocks
    private EpochService epochService;

    @Test
    void toEpochId_shouldReturnYYYYMM() {
        assertEquals(202604L, EpochService.toEpochId(Instant.parse("2026-04-15T12:00:00Z")));
        assertEquals(202612L, EpochService.toEpochId(Instant.parse("2026-12-01T00:00:00Z")));
        assertEquals(202601L, EpochService.toEpochId(Instant.parse("2026-01-31T23:59:59Z")));
    }

    @Test
    void toEpochId_boundaryAtMonthStart_isCorrect() {
        // Midnight UTC on 1 April 2026 - first instant of April
        assertEquals(202604L, EpochService.toEpochId(Instant.parse("2026-04-01T00:00:00Z")));
    }

    @Test
    void getCurrentEpochId_shouldBeValidYYYYMMFormat() {
        long id = EpochService.getCurrentEpochId();
        long year = id / 100;
        long month = id % 100;
        assertTrue(year >= 2024 && year <= 2099, "Year out of expected range: " + year);
        assertTrue(month >= 1 && month <= 12,    "Month out of range: " + month);
    }

    @Test
    void recordParticipation_newRecord_savesEntityWithCountOne() {
        UUID validatorId = UUID.randomUUID();
        String platformUrl = "example.com";
        long epochId = 202604L;
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
        long epochId = 202604L;
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
        long epochId = 202604L;
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