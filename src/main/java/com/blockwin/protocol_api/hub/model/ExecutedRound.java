package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.consensus.model.ConsensusResult;

import java.time.Instant;
import java.util.List;

public record ExecutedRound(long roundId, String platformUrl, Instant startTimestamp, Instant expiration, List<ConsensusResult> consensusResults) {
}
