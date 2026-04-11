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
public class PlatformEpochId implements Serializable {

    @Column(name = "platform_id", nullable = false)
    private UUID platformId;

    @Column(name = "epoch_id", nullable = false)
    private long epochId;
}
