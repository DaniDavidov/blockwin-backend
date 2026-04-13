package com.blockwin.protocol_api.reward.repository;

import com.blockwin.protocol_api.reward.model.PlatformEpochEntity;
import com.blockwin.protocol_api.reward.model.PlatformEpochId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface PlatformEpochRepository extends JpaRepository<PlatformEpochEntity, PlatformEpochId> {

    /**
     * Returns {@code true} if the platform has an epoch whose validation
     * window has not yet expired. Translates to a single indexed range scan and stops at the first match.
     */
    boolean existsByIdPlatformIdAndValidationEndTimestampAfter(UUID platformId, Instant now);
}
