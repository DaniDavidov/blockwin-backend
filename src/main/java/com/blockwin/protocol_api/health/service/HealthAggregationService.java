package com.blockwin.protocol_api.health.service;

import com.blockwin.protocol_api.health.model.RoundHealthEntity;
import com.blockwin.protocol_api.health.model.RoundId;
import com.blockwin.protocol_api.health.repository.RoundHealthRepository;
import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.hub.model.ExecutedRound;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class HealthAggregationService {
    public final Map<ReportType, HealthService> healthServices;
    public final RoundHealthRepository roundHealthRepository;

    @Autowired
    public HealthAggregationService(RoundHealthRepository roundHealthRepository, UptimeHealthService uptimeHealthService) {
        this.roundHealthRepository = roundHealthRepository;
        this.healthServices = new HashMap<>();
        this.healthServices.put(ReportType.UPTIME, uptimeHealthService);
    }

    private void persistExecutedRound(ExecutedRound executedRound) {
        RoundHealthEntity roundHealthEntity = RoundHealthEntity.builder()
                .roundId(new RoundId(executedRound.roundId(), executedRound.platformUrl()))
                .build();
        roundHealthRepository.save(roundHealthEntity);
    }

    @KafkaListener(
            topics = "round.execution",
            groupId = "health-aggregation-group"
    )
    @Transactional
    public void handle(ExecutedRound round, Acknowledgment ack) {
        RoundId roundId = new RoundId(round.roundId(), round.platformUrl());
        Optional<RoundHealthEntity> roundOpt = roundHealthRepository.findById(roundId);
        if (roundOpt.isPresent()) {
            log.atError().log("Round already accounted");
            return;
        }
        round.consensusResults().forEach(result -> {
            ReportType mechanismType = result.getMechanismType();
            HealthService healthService = healthServices.get(mechanismType);
            healthService.accountHealth(round.roundId(), round.platformUrl(), result);
        });

        persistExecutedRound(round);

        ack.acknowledge();
    }
}
