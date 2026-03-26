package com.blockwin.protocol_api.consensus.model;

import com.blockwin.protocol_api.hub.ReportType;

import java.util.Map;
import java.util.UUID;

public interface ConsensusResult {
    ReportType getMechanismType();
    Map<UUID, Boolean> getValidatorCorrectness();
}
