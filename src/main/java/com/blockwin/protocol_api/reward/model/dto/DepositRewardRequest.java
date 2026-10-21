package com.blockwin.protocol_api.reward.model.dto;

import com.blockwin.protocol_api.reward.model.enums.ValidationPeriodUnit;

/**
 * Request body for the reward deposit endpoint.
 *
 * @param txHash               on-chain transaction hash of the depositReward() call
 * @param chainName            logical chain name (must match a key in blockchain.chains config)
 * @param platformOwnerAddress Ethereum address of the platform owner who sent the transaction
 * @param validationPeriod     duration of the validation period, in {@code validationPeriodUnit}s
 * @param validationPeriodUnit unit of {@code validationPeriod} (MINUTES, HOURS or DAYS)
 */
public record DepositRewardRequest(
        String txHash,
        String chainName,
        String platformOwnerAddress,
        long validationPeriod,
        ValidationPeriodUnit validationPeriodUnit,
        String signature
) {}
