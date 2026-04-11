package com.blockwin.protocol_api.reward.model.dto;

import java.util.List;

/**
 * Response payload given to a validator so they can call claim() on the reward contract.
 *
 * @param websiteId        hex bytes32 encoding of the platform UUID
 * @param epochId          YYYYMM epoch identifier
 * @param validatorAddress Ethereum address of the validator
 * @param rewardAmount     reward in wei (as decimal string to avoid JS precision loss)
 * @param proof            ordered list of hex-encoded bytes32 sibling hashes
 * @param merkleRoot       hex bytes32 root stored on-chain for reference
 */
public record MerkleProofResponse(
        String websiteId,
        long epochId,
        String validatorAddress,
        String rewardAmount,
        List<String> proof,
        String merkleRoot
) {}
