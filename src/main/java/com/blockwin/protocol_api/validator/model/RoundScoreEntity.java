package com.blockwin.protocol_api.validator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "platform_rounds")
public class RoundScoreEntity {

    @EmbeddedId
    private PlatformRoundId roundId;

    @Column(name = "from_timestamp", nullable = false)
    private Instant fromTimestamp;

    @Column(name = "to_timestamp", nullable = false)
    private Instant toTimestamp;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

}
