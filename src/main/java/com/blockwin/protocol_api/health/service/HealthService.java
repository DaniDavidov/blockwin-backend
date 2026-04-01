package com.blockwin.protocol_api.health.service;

import com.blockwin.protocol_api.consensus.model.ConsensusResult;

public interface HealthService {
    void accountHealth(long roundId, String platformUrl, ConsensusResult consensusResult);
}