package com.blockwin.protocol_api.reward.model;

import com.blockwin.protocol_api.validator.model.enums.ChainName;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;
import java.time.Instant;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "platform_epochs",
        indexes = @Index(name = "idx_platform_epochs_platform_end", columnList = "platform_id, validation_end_timestamp")
)
public class PlatformEpochEntity {

    @EmbeddedId
    private PlatformEpochId id;

    /**
     * Transaction hash of the on-chain depositReward() call. Unique across all epochs so
     * the same tx cannot fund two different epochs.
     */
    @Column(name = "deposit_tx_hash", length = 66, unique = true, nullable = false)
    private String depositTxHash;

    /**
     * Total reward pool deposited by the platform owner for this epoch (in wei).
     */
    @Column(name = "reward_pot", precision = 78, scale = 0, nullable = false)
    private BigInteger rewardPot;


    @Column(name = "validation_end_timestamp", nullable = false)
    private Instant validationEndTimestamp;

    @Column(name = "validation_start_timestamp", nullable = false)
    private Instant validationStartTimestamp;

    /**
     * The chain on which the Merkle root was published and against which validator
     * addresses were resolved. Required to reconstruct the tree correctly when
     * generating proofs for a multichain deployment.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "chain_name", length = 32)
    private ChainName chainName;

    /**
     * Hex-encoded bytes32 Merkle root, set when the epoch is closed.
     * Format: "0x" + 64 hex chars.
     */
    @Column(name = "merkle_root", length = 66)
    private String merkleRoot;

    /**
     * True once the Merkle root has been submitted on-chain via publishRoot().
     */
    @Column(name = "published", nullable = false)
    private boolean published;

    /**
     * Timestamp when closeEpoch() was called and the Merkle tree was computed.
     */
    @Column(name = "closed_at")
    private Instant closedAt;
}
