package com.blockwin.protocol_api.validator.repository;

import com.blockwin.protocol_api.validator.model.ValidatorChainEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ValidatorChainRepository extends JpaRepository<ValidatorChainEntity, UUID> {

    /**
     * Returns all chain identity records for a given validator UUID.
     * Used by RewardService to resolve the validator's Ethereum address.
     */
    List<ValidatorChainEntity> findByValidator_Uuid(UUID validatorId);

    Optional<ValidatorChainEntity> findByPublicKey(String publicKey);
}
