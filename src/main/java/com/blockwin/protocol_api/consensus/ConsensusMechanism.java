package com.blockwin.protocol_api.consensus;

import com.blockwin.protocol_api.consensus.model.ConsensusResult;
import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.hub.model.Report;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ConsensusMechanism<T extends Report, R extends ConsensusResult> {
    public R execute(List<T> reports, Map<UUID, Integer> reputationsByIds);

    ReportType supportedType();
}
