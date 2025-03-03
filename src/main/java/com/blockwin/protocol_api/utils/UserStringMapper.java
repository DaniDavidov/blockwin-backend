package com.blockwin.protocol_api.utils;


import com.blockwin.protocol_api.model.entity.UserEntity;
import com.blockwin.protocol_api.model.entity.UserRoleEntity;
import com.blockwin.protocol_api.model.entity.enums.UserRoleEnum;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class UserStringMapper {

    public static String map(UserEntity user) {
        String roles = user.getUserRoles().stream().map(role -> role.getName().name()).collect(Collectors.joining(","));
        return user.getEmail() +
                "|" + user.getPassword() +
                "|" + user.getFirstName() +
                "|" + user.getLastName() +
                "|" + user.getPhoneNumber() +
                "|" + user.getCreatedAt() +
                "|" + user.getUpdatedAt() +
                "|" + roles;
    }

    public static UserEntity map(String user) {
        UserEntity userEntity = new UserEntity();
        String[] tokens = user.split("\\|");
        userEntity.setEmail(tokens[0]);
        userEntity.setPassword(tokens[1]);
        userEntity.setFirstName(tokens[2]);
        userEntity.setLastName(tokens[3]);
        userEntity.setPhoneNumber(tokens[4]);
        userEntity.setCreatedAt(LocalDate.parse(tokens[5]));
        userEntity.setUpdatedAt(LocalDate.parse(tokens[6]));
        String[] strRoles = tokens[7].split(",");
        Set<UserRoleEntity> roles = Arrays.stream(strRoles)
                .map(strRole -> new UserRoleEntity(UserRoleEnum.valueOf(strRole)))
                .collect(Collectors.toSet());
        userEntity.setUserRoles(roles);
        return userEntity;
    }
}
