package com.blockwin.protocol_api.validator.service;

import com.blockwin.protocol_api.consensus.cache.ValidatorReputationCacheService;
import com.blockwin.protocol_api.consensus.model.ConsensusResult;
import com.blockwin.protocol_api.hub.model.ExecutedRound;
import com.blockwin.protocol_api.reward.service.EpochService;
import com.blockwin.protocol_api.validator.model.PlatformRoundId;
import com.blockwin.protocol_api.validator.model.RoundScoreEntity;
import com.blockwin.protocol_api.validator.model.ValidatorEntity;
import com.blockwin.protocol_api.validator.repository.RoundScoreRepository;
import com.blockwin.protocol_api.validator.repository.ValidatorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.blockwin.protocol_api.common.utils.Constants.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ValidatorScoringService {
    private final ValidatorRepository validatorRepository;
    private final RoundScoreRepository roundScoreRepository;
    private final ValidatorReputationCacheService reputationCacheService;
    private final EpochService epochService;

    public int getValidatorReputation(UUID validatorId) {
        Optional<ValidatorEntity> opt = validatorRepository.findById(validatorId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("No validator with id " + validatorId);
        }
        ValidatorEntity validatorEntity = opt.get();
        long correctReports = validatorEntity.getCorrectReports();
        long totalReports = validatorEntity.getTotalReports();
        return (int) (correctReports * MAX_BPS / totalReports);
    }

    private void updateValidatorReputation(UUID validatorId, Boolean isCorrect) {
        Optional<ValidatorEntity> validatorOpt = validatorRepository.findById(validatorId);
        if (validatorOpt.isEmpty()) {
            return;
        }
        ValidatorEntity validatorEntity = validatorOpt.get();
        if (isCorrect) {
            validatorEntity.setCorrectReports(validatorEntity.getCorrectReports() + 1);
        }
        validatorEntity.setTotalReports(validatorEntity.getTotalReports() + 1);
        long correctReports = validatorEntity.getCorrectReports() + VIRTUAL_CORRECT_REPORTS;
        long totalReports = validatorEntity.getTotalReports() + VIRTUAL_TOTAL_REPORTS;
        int reputation = (int) (correctReports * MAX_BPS / totalReports);
        validatorRepository.save(validatorEntity);
        reputationCacheService.cacheValidator(validatorId, reputation);
    }

    private void persistExecutedRound(ExecutedRound executedRound) {
        RoundScoreEntity roundScoreEntity = RoundScoreEntity.builder()
                .roundId(new PlatformRoundId(executedRound.roundId(), executedRound.platformUrl()))
                .fromTimestamp(executedRound.startTimestamp())
                .toTimestamp(executedRound.expiration())
                .executedAt(Instant.now())
                .build();
        roundScoreRepository.save(roundScoreEntity);
    }

    @KafkaListener(
            topics = "round.execution",
            groupId = "validator-scoring-group"
    )
    @Transactional
    public void handle(ExecutedRound round, Acknowledgment ack) {
        if ((roundScoreRepository.findById(new PlatformRoundId(round.roundId(), round.platformUrl()))).isPresent()) {
            log.atWarn().log("Duplicate round.execution event - already processed, skipping");
            ack.acknowledge();
            return;
        }
        long epochId = EpochService.toEpochId(round.startTimestamp());
        for (ConsensusResult r : round.consensusResults()) {
            r.getValidatorCorrectness().forEach((validatorId, isCorrect) -> {
                updateValidatorReputation(validatorId, isCorrect);
                epochService.recordParticipation(validatorId, round.platformUrl(), epochId);
            });
        }
        persistExecutedRound(round);
        ack.acknowledge();
    }
}
