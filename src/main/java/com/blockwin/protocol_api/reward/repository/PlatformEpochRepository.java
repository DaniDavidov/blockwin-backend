package com.blockwin.protocol_api.reward.repository;

import com.blockwin.protocol_api.reward.model.PlatformEpochEntity;
import com.blockwin.protocol_api.reward.model.PlatformEpochId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformEpochRepository extends JpaRepository<PlatformEpochEntity, PlatformEpochId> {
}
