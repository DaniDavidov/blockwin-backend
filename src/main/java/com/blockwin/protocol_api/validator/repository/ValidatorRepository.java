package com.blockwin.protocol_api.validator.repository;

import com.blockwin.protocol_api.validator.model.ValidatorEntity;
import com.blockwin.protocol_api.validator.model.enums.ValidatorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ValidatorRepository extends JpaRepository<ValidatorEntity, UUID> {

    @Modifying
    @Query("UPDATE ValidatorEntity ve SET ve.status = :to WHERE ve.status = :from")
    int resetStatus(@Param("from") ValidatorStatus from, @Param("to") ValidatorStatus to);
}
