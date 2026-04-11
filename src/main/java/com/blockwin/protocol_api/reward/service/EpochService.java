package com.blockwin.protocol_api.reward.service;

import com.blockwin.protocol_api.reward.model.EpochParticipationEntity;
import com.blockwin.protocol_api.reward.model.EpochParticipationId;
import com.blockwin.protocol_api.reward.repository.EpochParticipationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages epoch lifecycle and validator participation tracking.
 *
 * <p>Epoch ID format: {@code YYYYMM} (e.g. {@code 202604} for April 2026).
 * This keeps epochs human-readable and naturally ordered.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EpochService {

    private final EpochParticipationRepository epochParticipationRepository;

    /**
     * Derives the epoch ID (YYYYMM) from an arbitrary instant in UTC.
     */
    public static long toEpochId(Instant timestamp) {
        var dt = timestamp.atZone(ZoneOffset.UTC);
        return (long) dt.getYear() * 100 + dt.getMonthValue();
    }

    /**
     * Returns the current epoch ID based on wall-clock time.
     */
    public static long getCurrentEpochId() {
        return toEpochId(Instant.now());
    }

    /**
     * Records one round of participation for a validator in a given platform/epoch.
     * Called from {@link com.blockwin.protocol_api.validator.service.ValidatorScoringService}
     * on every {@code round.execution} Kafka event.
     *
     * <p>Uses a read-then-update strategy within the caller's existing transaction.
     */
    public void recordParticipation(UUID validatorId, String platformUrl, long epochId) {
        EpochParticipationId id = new EpochParticipationId(validatorId, platformUrl, epochId);
        Optional<EpochParticipationEntity> existing = epochParticipationRepository.findById(id);

        if (existing.isPresent()) {
            EpochParticipationEntity entity = existing.get();
            entity.setRoundsParticipated(entity.getRoundsParticipated() + 1);
            epochParticipationRepository.save(entity);
        } else {
            epochParticipationRepository.save(
                    EpochParticipationEntity.builder()
                            .id(id)
                            .roundsParticipated(1L)
                            .build()
            );
        }

        log.debug("Recorded participation: validator={} platform={} epoch={}", validatorId, platformUrl, epochId);
    }
}
