package com.blockwin.protocol_api.validator.repository;

import com.blockwin.protocol_api.validator.model.RoundScoreEntity;
import com.blockwin.protocol_api.validator.model.PlatformRoundId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoundScoreRepository extends JpaRepository<RoundScoreEntity, PlatformRoundId> {
}
