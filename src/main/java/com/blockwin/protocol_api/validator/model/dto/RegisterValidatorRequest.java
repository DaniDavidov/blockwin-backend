package com.blockwin.protocol_api.validator.model.dto;

import com.blockwin.protocol_api.validator.model.enums.ChainName;

import java.util.UUID;

public record RegisterValidatorRequest(
        String ipAddress,
        String continent,
        String country,
        UUID validatorChainUuid,
        String chainId,
        ChainName chainName,
        String publicKey,
        String txHash,
        String signedMessage,
        Integer capacity
) {}
