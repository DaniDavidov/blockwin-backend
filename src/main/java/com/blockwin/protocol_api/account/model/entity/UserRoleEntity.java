package com.blockwin.protocol_api.account.model.entity;


import com.blockwin.protocol_api.account.model.entity.enums.UserRoleEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_roles")
public class UserRoleEntity extends BaseEntity {

    @Column(name = "user_role", nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private UserRoleEnum name;


}
