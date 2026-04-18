package com.blockwin.protocol_api.reward.service;

import com.blockwin.protocol_api.reward.model.EpochParticipationId;
import com.blockwin.protocol_api.reward.model.dto.ActiveEpoch;
import com.blockwin.protocol_api.reward.repository.PlatformEpochRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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

    private static final String UPSERT_PARTICIPATION_SQL = """
            INSERT INTO epoch_participation (validator_id, platform_url, epoch_id, rounds_participated)
            VALUES (:validatorId, :platformUrl, :epochId, :increment)
            ON CONFLICT (validator_id, platform_url, epoch_id)
            DO UPDATE SET rounds_participated = epoch_participation.rounds_participated + EXCLUDED.rounds_participated
            """;

    private final PlatformEpochRepository platformEpochRepository;
    private final NamedParameterJdbcTemplate jdbc;

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
     * Returns all epochs whose validation window has not yet expired, projected
     * into {@link ActiveEpoch} records. Used by the startup recovery pass to
     * restore {@code RoundState} objects for all currently active platforms.
     */
    public List<ActiveEpoch> findAllActivePlatformEpochs() {
        return platformEpochRepository.findAllByValidationEndTimestampAfter(Instant.now())
                .stream()
                .map(e -> new ActiveEpoch(
                        e.getId().getPlatformId(),
                        e.getValidationStartTimestamp(),
                        e.getValidationEndTimestamp()))
                .toList();
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
     * Records participation increments for a batch of validator/platform/epoch keys
     * in a single round trip. New rows are inserted with the supplied participation count.
     * Existing rows get their {@code rounds_participated} increased by the same count
     *
     * <p>Participates in the caller's transaction because the underlying DataSource
     * is Spring-managed.
     */
    public void recordParticipationBatch(Map<EpochParticipationId, Long> increments) {
        if (increments.isEmpty()) {
            return;
        }
        SqlParameterSource[] params = increments.entrySet().stream()
                .map(e -> new MapSqlParameterSource()
                        .addValue("validatorId", e.getKey().getValidatorId())
                        .addValue("platformUrl", e.getKey().getPlatformUrl())
                        .addValue("epochId", e.getKey().getEpochId())
                        .addValue("increment", e.getValue()))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(UPSERT_PARTICIPATION_SQL, params);
    }
}
