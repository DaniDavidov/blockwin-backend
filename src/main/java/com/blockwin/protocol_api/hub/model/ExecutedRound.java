package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.consensus.model.ConsensusResult;

import java.util.List;

public record ExecutedRound(long roundId, List<ConsensusResult> consensusResults) {
}
