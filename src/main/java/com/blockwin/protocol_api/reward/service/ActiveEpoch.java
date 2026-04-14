package com.blockwin.protocol_api.reward.service;

import java.time.Instant;
import java.util.UUID;

public record ActiveEpoch(
        UUID platformId,
        Instant validationStartTimestamp,
        Instant validationEndTimestamp) {}
