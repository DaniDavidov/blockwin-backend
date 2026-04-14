package com.blockwin.protocol_api.reward.model.dto;

import java.time.Instant;
import java.util.UUID;

public record ActiveEpoch(
        UUID platformId,
        Instant validationStartTimestamp,
        Instant validationEndTimestamp) {}
