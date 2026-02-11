package com.blockwin.protocol_api.validator.repository;

import com.blockwin.protocol_api.validator.model.ValidatorChainEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ValidatorChainRepository extends JpaRepository<ValidatorChainEntity, UUID> {
}
