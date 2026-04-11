package com.blockwin.protocol_api.reward.repository;

import com.blockwin.protocol_api.reward.model.EpochParticipationEntity;
import com.blockwin.protocol_api.reward.model.EpochParticipationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpochParticipationRepository extends JpaRepository<EpochParticipationEntity, EpochParticipationId> {

    /**
     * Fetch all validator participation records for a specific platform and epoch.
     * Used when building the Merkle tree at epoch close.
     */
    List<EpochParticipationEntity> findByIdPlatformUrlAndIdEpochId(String platformUrl, long epochId);
}
