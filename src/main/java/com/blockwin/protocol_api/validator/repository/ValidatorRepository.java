package com.blockwin.protocol_api.validator.repository;

import com.blockwin.protocol_api.validator.model.ValidatorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ValidatorRepository extends JpaRepository<ValidatorEntity, UUID> {
}
