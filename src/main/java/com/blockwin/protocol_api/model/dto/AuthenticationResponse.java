package com.blockwin.protocol_api.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthenticationResponse {
    private String token;
    private Long userId;
    private String message;
}
