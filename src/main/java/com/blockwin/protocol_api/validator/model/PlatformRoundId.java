package com.blockwin.protocol_api.validator.model;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Embeddable
public class PlatformRoundId implements Serializable {
    private long roundId;
    private String platformUrl;
}
