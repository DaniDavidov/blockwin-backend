package com.blockwin.protocol_api.reward.service;

import com.blockwin.protocol_api.reward.model.EpochParticipationEntity;
import com.blockwin.protocol_api.reward.model.EpochParticipationId;
import com.blockwin.protocol_api.reward.repository.EpochParticipationRepository;
import com.blockwin.protocol_api.reward.repository.PlatformEpochRepository;
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
 * <p>Epoch ID format: {@code YYYYMMDD} (e.g. {@code 20260401} for 1 April 2026).
 * Day granularity keeps IDs human-readable and supports multiple validation
 * periods within the same calendar month (e.g. four weekly epochs).
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EpochService {

    private final EpochParticipationRepository epochParticipationRepository;
    private final PlatformEpochRepository platformEpochRepository;

    /**
     * Derives the epoch ID (YYYYMMDD) from an arbitrary instant in UTC.
     *
     * <p>Day granularity supports sub-monthly validation periods (e.g. four
     * consecutive weekly epochs within the same calendar month all receive
     * distinct IDs: 20260401, 20260408, 20260415, 20260422).
     */
    public static long toEpochId(Instant timestamp) {
        var dt = timestamp.atZone(ZoneOffset.UTC);
        return (long) dt.getYear() * 10_000L
                + (long) dt.getMonthValue() * 100L
                + dt.getDayOfMonth();
    }

    /**
     * Returns the current epoch ID based on wall-clock time.
     */
    public static long getCurrentEpochId() {
        return toEpochId(Instant.now());
    }

    /**
     * Returns {@code true} if the platform currently has at least one epoch whose
     * validation window has not yet expired. Used to gate platform-update event
     * emission in {@link com.blockwin.protocol_api.platform.service.PlatformService}.
     */
    public boolean hasActiveValidationPeriod(UUID platformId) {
        return platformEpochRepository
                .existsByIdPlatformIdAndValidationEndTimestampAfter(platformId, Instant.now());
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
