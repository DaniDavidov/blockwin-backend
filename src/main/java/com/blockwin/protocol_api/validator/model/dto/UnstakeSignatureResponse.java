package com.blockwin.protocol_api.validator.model.dto;

import java.math.BigInteger;

public record UnstakeSignatureResponse(
        String validatorAddress,
        String chainName,
        BigInteger unstakableAmount,
        String signature
) {}
