package com.blockwin.protocol_api.health.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
public class RoundHealthEntity {
    @EmbeddedId
    @Column(name = "round_id")
    private RoundId roundId;

    @Column(name = "voted_category", nullable = false)
    private String votedCategory;

    @Column(nullable = false)
    private int certainty;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
