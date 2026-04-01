package com.blockwin.protocol_api.health.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Embeddable
public class RoundId implements Serializable {
    private long roundId;
    private String platformUrl;
}
