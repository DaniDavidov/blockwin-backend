package com.blockwin.protocol_api.reward.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "epoch_participation")
public class EpochParticipationEntity {

    @EmbeddedId
    private EpochParticipationId id;

    @Column(name = "rounds_participated", nullable = false)
    private long roundsParticipated;

    @Column(name = "reward_amount", precision = 78, scale = 0)
    private BigInteger rewardAmount;
}
