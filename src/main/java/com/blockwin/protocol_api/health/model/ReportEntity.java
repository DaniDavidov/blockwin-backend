package com.blockwin.protocol_api.health.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "platforms")
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private long dns;

    @Column(nullable = false)
    private long tcp;

    @Column(nullable = false)
    private long tls;

    @Column(nullable = false)
    private long ttfb;

    @Column(nullable = false)
    private long latency;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "platform_url", nullable = false)
    private String platformUrl;

    @Column(name = "validator_id", nullable = false)
    private UUID validatorId;

    @Column(name = "round_id", nullable = false)
    private long roundId;

}
