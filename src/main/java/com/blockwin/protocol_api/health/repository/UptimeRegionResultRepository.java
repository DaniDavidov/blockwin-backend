package com.blockwin.protocol_api.health.repository;

import com.blockwin.protocol_api.health.model.UptimeRegionResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UptimeRegionResultRepository extends JpaRepository<UptimeRegionResultEntity, UUID> {
}
