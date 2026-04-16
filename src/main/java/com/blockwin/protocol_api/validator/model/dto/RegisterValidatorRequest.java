package com.blockwin.protocol_api.validator.model.dto;

public record RegisterValidatorRequest(
        String ipAddress,
        String continent,
        String country,
        String chainId,
        String chainName,
        String publicKey,
        Integer capacity
) {}