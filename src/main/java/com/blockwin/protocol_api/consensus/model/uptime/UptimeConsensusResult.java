package com.blockwin.protocol_api.consensus.model.uptime;

import com.blockwin.protocol_api.consensus.model.ConsensusResult;
import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.hub.model.UptimeReport;
import com.blockwin.protocol_api.validator.model.enums.Continent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UptimeConsensusResult(
        ReportType mechanismType,
        ReportCategory winnerCategory,
        int certainty,
        Map<Continent, ReportCategory> categoryByRegion,
        Map<Continent, Latency> latencyByRegion,
        Map<UUID, Boolean> validatorCorrectness,
        List<UptimeReport> reports
) implements ConsensusResult {
    @Override
    public ReportType getMechanismType() {
        return mechanismType;
    }

    @Override
    public Map<UUID, Boolean> getValidatorCorrectness() {
        return validatorCorrectness;
    }

}
