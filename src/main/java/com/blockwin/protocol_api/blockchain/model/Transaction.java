package com.blockwin.protocol_api.blockchain.model;

import com.blockwin.protocol_api.validator.model.enums.ChainName;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tx_hash", "chain_name"})
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(name = "tx_hash", nullable = false)
    private String txHash;

    @Column(name = "validator_address", nullable = false)
    private String validatorAddress;

    @Column(name = "chain_name", nullable = false)
    @Enumerated(EnumType.STRING)
    private ChainName chainName;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private BigInteger amount;
}

