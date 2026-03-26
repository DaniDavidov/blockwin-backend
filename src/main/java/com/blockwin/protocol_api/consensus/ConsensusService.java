package com.blockwin.protocol_api.consensus;

import com.blockwin.protocol_api.consensus.cache.ValidatorReputationCacheService;
import com.blockwin.protocol_api.consensus.model.ConsensusResult;
import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.hub.model.Report;
import com.blockwin.protocol_api.hub.model.RoundState;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ConsensusService {
    private final Map<ReportType, ConsensusMechanism<?, ?>> mechanisms;
    private final ValidatorReputationCacheService validatorReputationCacheService;

    public ConsensusService(List<ConsensusMechanism<?, ?>> mechanisms, ValidatorReputationCacheService validatorReputationCacheService) {
        Map<ReportType, ConsensusMechanism<?, ?>> map = new EnumMap<>(ReportType.class);

        for (ConsensusMechanism<?, ?> mechanism : mechanisms) {
            map.put(mechanism.supportedType(), mechanism);
        }

        this.mechanisms = Map.copyOf(map);
        this.validatorReputationCacheService = validatorReputationCacheService;
    }

    public List<ConsensusResult> executeRound(RoundState state) {
        Set<UUID> ids = new HashSet<>(state.getBitmapByValidator().keySet());
        Map<UUID, Integer> reputationsByIds = validatorReputationCacheService.fetchReputations(ids);
        List<ConsensusResult> results = new ArrayList<>();

        for (Map.Entry<ReportType, List<Report>> entry : state.getReportsByType().entrySet()) {

            ReportType reportType = entry.getKey();
            List<Report> reports = entry.getValue();

            @SuppressWarnings("unchecked")
            ConsensusMechanism<Report, ? extends ConsensusResult> mechanism =
                    (ConsensusMechanism<Report, ? extends ConsensusResult>) mechanisms.get(reportType);

            results.add(mechanism.execute(reports, reputationsByIds));
        }

        return results;
    }
}
