package com.blockwin.protocol_api.validator.model;

import com.blockwin.protocol_api.validator.model.enums.ChainName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "validator_chain_identity", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chain_id", "public_key"})
})
public class ValidatorChainEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(name = "chain_id", nullable = false, length = 50)
    private String chainId;

    @Column(name = "chain_name", nullable = false)
    @Enumerated(EnumType.STRING)
    private ChainName chainName;

    @Column(name = "public_key", nullable = false, length = 80)
    private String publicKey;

    @ManyToOne
    @JoinColumn(name = "validator_id", nullable = false)
    private ValidatorEntity validator;

}
