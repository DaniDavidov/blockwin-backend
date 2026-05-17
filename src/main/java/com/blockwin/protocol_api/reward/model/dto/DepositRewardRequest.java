package com.blockwin.protocol_api.reward.model.dto;

/**
 * Request body for the reward deposit endpoint.
 *
 * @param txHash               on-chain transaction hash of the depositReward() call
 * @param chainName            logical chain name (must match a key in blockchain.chains config)
 * @param platformOwnerAddress Ethereum address of the platform owner who sent the transaction
 * @param validationDays       duration of the validation period in days
 */
public record DepositRewardRequest(
        String txHash,
        String chainName,
        String platformOwnerAddress,
        int validationDays,
        String signature
) {}