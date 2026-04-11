package com.blockwin.protocol_api.reward.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class EpochParticipationId implements Serializable {

    @Column(name = "validator_id", nullable = false)
    private UUID validatorId;

    @Column(name = "platform_url", nullable = false, length = 512)
    private String platformUrl;

    @Column(name = "epoch_id", nullable = false)
    private long epochId;
}
