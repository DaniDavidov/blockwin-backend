package com.blockwin.protocol_api.validator.model.dto;

import com.blockwin.protocol_api.validator.model.enums.ChainName;

import java.util.UUID;

public record RegisterValidatorRequest(
        String ipAddress,
        String continent,
        String country,
        String chainId,
        String chainName,
        String publicKey,
        String txHash,
        String message,
        String signature,
        Integer capacity
) {}
