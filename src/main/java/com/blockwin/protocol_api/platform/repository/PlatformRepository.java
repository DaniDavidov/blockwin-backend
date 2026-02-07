package com.blockwin.protocol_api.platform.repository;

import com.blockwin.protocol_api.platform.model.PlatformEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformRepository extends JpaRepository<PlatformEntity, UUID> {
    public Optional<PlatformEntity> findByUrl(String url);
}
