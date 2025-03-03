package com.blockwin.protocol_api.repository;

import com.blockwin.protocol_api.model.entity.UserRoleEntity;
import com.blockwin.protocol_api.model.entity.enums.UserRoleEnum;
import org.springframework.data.repository.CrudRepository;

import java.nio.CharBuffer;
import java.util.Optional;

public interface UserRoleRepository extends CrudRepository<UserRoleEntity, Long> {
    Optional<UserRoleEntity> findById(Long id);

    Optional<UserRoleEntity> findByName(UserRoleEnum userRoleEnum);
}
