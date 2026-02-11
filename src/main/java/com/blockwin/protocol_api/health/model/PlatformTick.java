package com.blockwin.protocol_api.health.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "platforms")
public class PlatformTick {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "platform_id", nullable = false)
    private UUID platformId;

    @Column(name = "validator_id", nullable = false)
    private UUID validatorId;

}
