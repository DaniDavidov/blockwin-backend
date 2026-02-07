package com.blockwin.protocol_api.account.repository;

import com.blockwin.protocol_api.account.model.entity.UserRoleEntity;
import com.blockwin.protocol_api.account.model.entity.enums.UserRoleEnum;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends CrudRepository<UserRoleEntity, UUID> {
    Optional<UserRoleEntity> findById(UUID id);

    Optional<UserRoleEntity> findByName(UserRoleEnum userRoleEnum);
}
