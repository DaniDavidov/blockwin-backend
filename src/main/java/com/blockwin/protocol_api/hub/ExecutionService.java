package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.consensus.ConsensusService;
import com.blockwin.protocol_api.consensus.model.ConsensusResult;
import com.blockwin.protocol_api.hub.model.ExecutedRound;
import com.blockwin.protocol_api.hub.model.RoundState;
import com.blockwin.protocol_api.platform.event.PlatformUpdateEvent;
import com.blockwin.protocol_api.reward.service.EpochService;
import com.blockwin.protocol_api.reward.service.RewardService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ExecutionService {
    private static final int NUMBER_OF_WORKERS = 5;
    private final StateRegistry stateRegistry;
    private final StateUpdateRegistry stateUpdateRegistry;
    private final ConsensusService consensusService;
    private final RewardService rewardService;
    private final BroadcastService broadcastService;
    private final KafkaTemplate<String, ExecutedRound> kafkaTemplate;

    @PostConstruct
    public void initializeWorkers() {
        for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
            initWorker();
        }
    }

    private void notifyForConsensusResult(ExecutedRound executedRound) {
        kafkaTemplate.send(
                "round.execution",
                String.valueOf(executedRound.roundId()),
                executedRound
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish round execution event", ex);
            } else {
                log.debug("Event sent for round {}", executedRound.roundId());
            }
        });
    }

    private void executeEpoch(RoundState roundState) {
        // Notify validators first so they stop submitting reports immediately.
        broadcastService.broadcastPlatformExpired(roundState.getPlatformURL());

        stateRegistry.removeState(roundState.getPlatformURL());
        stateUpdateRegistry.removeUpdate(roundState.getPlatformURL());

        UUID platformId = roundState.getPlatformId();
        long epochId = EpochService.toEpochId(roundState.getExpiration());
        try {
            rewardService.closeEpoch(platformId, epochId);
            log.info("Epoch closed: platform={} epochId={}", roundState.getPlatformURL(), epochId);
        } catch (Exception e) {
            // State is already cleared — log and continue so the worker thread survives.
            log.error("Failed to close epoch for platform={} epochId={}: {}",
                    roundState.getPlatformURL(), epochId, e.getMessage());
        }
    }

    private void resetRound(RoundState roundState) {
        if (roundState.getLastRoundId() == roundState.getRoundId()) {
            executeEpoch(roundState);
            return;
        }
        long nextRoundId = roundState.getRoundId() + 1;
        PlatformUpdateEvent update = stateUpdateRegistry.getUpdate(roundState.getPlatformURL());
        RoundState newRound;
        if (update == null) {
            newRound = new RoundState(
                    roundState.getPlatformId(),
                    roundState.getPlatformURL(),
                    roundState.getCheckIntervalSeconds(),
                    roundState.getRegistrationTimestamp(),
                    roundState.getLastRoundId(),
                    nextRoundId
            );
        } else {
            newRound = new RoundState(
                    roundState.getPlatformId(),
                    update.getPlatformURL(),
                    update.getCheckIntervalSeconds(),
                    update.getRegistrationTimestamp(),
                    roundState.getLastRoundId(),
                    nextRoundId
            );
            stateUpdateRegistry.removeUpdate(update.getPlatformURL());
        }
        stateRegistry.resetState(newRound);
    }

    private void initWorker() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    RoundState roundState = this.stateRegistry.takeExpired();
                    this.stateRegistry.removeState(roundState.getPlatformURL());
                    resetRound(roundState);
                    List<ConsensusResult> consensusResults = this.consensusService.executeRound(roundState);
                    ExecutedRound executedRound = new ExecutedRound(
                            roundState.getRoundId(),
                            roundState.getPlatformURL(),
                            roundState.getStartTimestamp(),
                            roundState.getExpiration(),
                            consensusResults
                    );
                    notifyForConsensusResult(executedRound);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
