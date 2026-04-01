package com.blockwin.protocol_api.health.repository;

import com.blockwin.protocol_api.health.model.RoundHealthEntity;
import com.blockwin.protocol_api.health.model.RoundId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoundHealthRepository extends JpaRepository<RoundHealthEntity, RoundId> {
}
