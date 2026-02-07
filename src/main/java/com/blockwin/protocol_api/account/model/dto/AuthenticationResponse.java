package com.blockwin.protocol_api.account.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthenticationResponse {
    private String token;
    private UUID userId;
    private String message;
}
