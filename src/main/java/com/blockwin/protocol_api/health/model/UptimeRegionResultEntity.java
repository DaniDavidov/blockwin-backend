package com.blockwin.protocol_api.health.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "uptime_region_results")
public class UptimeRegionResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String category;

    private String country;
    private String continent;

    private long dns;
    private long tcp;
    private long tls;
    private long ttfb;
    private long total;

    @Column(name = "round_id", nullable = false)
    private long round;
}
