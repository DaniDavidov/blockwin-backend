package com.blockwin.protocol_api.token;

import com.blockwin.protocol_api.model.entity.BaseEntity;
import com.blockwin.protocol_api.model.entity.UserEntity;
import com.blockwin.protocol_api.model.entity.enums.TokenType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Token extends BaseEntity {

    private String token;

    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    private boolean expired;

    private boolean revoked;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
}
