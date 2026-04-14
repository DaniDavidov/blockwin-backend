package com.blockwin.protocol_api.reward.controller;

import com.blockwin.protocol_api.reward.model.dto.DepositRewardRequest;
import com.blockwin.protocol_api.reward.model.dto.MerkleProofResponse;
import com.blockwin.protocol_api.reward.service.RewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    private final RewardService rewardService;

    /**
     * Platform owner registers their on-chain reward deposit for a given epoch.
     * The request body must include the transaction hash of the {@code depositReward()} call
     * so the server can verify the event on-chain.
     */
    @PostMapping("/platforms/{platformId}/deposit")
    public ResponseEntity<Map<String, String>> depositReward(
            @PathVariable UUID platformId,
            @RequestBody DepositRewardRequest request) {
        try {
            rewardService.verifyRewardDeposit(platformId, request);
            return ResponseEntity.ok(Map.of("status", "deposit-verified"));
        } catch (IllegalStateException e) {
            log.error("Deposit rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to process reward deposit: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin publishes the stored Merkle root to the reward smart contract on-chain.
     *
     * @param chain logical chain name (default: "ethereum")
     * @return JSON with {@code txHash}
     */
    @PostMapping("/platforms/{platformId}/epochs/{epochId}/publish")
    public ResponseEntity<Map<String, String>> publishRoot(
            @PathVariable UUID platformId,
            @PathVariable long epochId,
            @RequestParam(defaultValue = "ethereum") String chain) {
        try {
            String txHash = rewardService.publishRoot(platformId, epochId, chain);
            return ResponseEntity.ok(Map.of("txHash", txHash));
        } catch (Exception e) {
            log.error("Failed to publish root: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Validator fetches their Merkle proof and reward amount for claiming on-chain.
     *
     * @param validatorAddress the validator's Ethereum address (0x-prefixed)
     */
    @GetMapping("/platforms/{platformId}/epochs/{epochId}/proof/{validatorAddress}")
    public ResponseEntity<MerkleProofResponse> getMerkleProof(
            @PathVariable UUID platformId,
            @PathVariable long epochId,
            @PathVariable String validatorAddress) {

        MerkleProofResponse response = rewardService.getMerkleProof(platformId, epochId, validatorAddress);
        return ResponseEntity.ok(response);
    }
}
