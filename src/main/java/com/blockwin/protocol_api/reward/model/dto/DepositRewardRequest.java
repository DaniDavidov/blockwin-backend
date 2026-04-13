package com.blockwin.protocol_api.reward.model.dto;

import java.math.BigInteger;

/**
 * Request body for the reward deposit endpoint.
 *
 * @param txHash               on-chain transaction hash of the depositReward() call
 * @param chainName            logical chain name (must match a key in blockchain.chains config)
 * @param platformOwnerAddress Ethereum address of the platform owner who sent the transaction
 */
public record DepositRewardRequest(
        String txHash,
        String chainName,
        String platformOwnerAddress,
        String platformUrl,
        BigInteger amount,
        int validationDays
) {}