package com.blockwin.protocol_api.reward.service;

import com.blockwin.protocol_api.blockchain.config.BlockchainConfig;
import com.blockwin.protocol_api.blockchain.service.MultiChainService;
import com.blockwin.protocol_api.blockchain.service.TransactionManagementService;
import com.blockwin.protocol_api.platform.event.CachePlatformEvent;
import com.blockwin.protocol_api.platform.model.PlatformEntity;
import com.blockwin.protocol_api.platform.repository.PlatformRepository;
import com.blockwin.protocol_api.reward.model.EpochParticipationEntity;
import com.blockwin.protocol_api.reward.model.EpochParticipationId;
import com.blockwin.protocol_api.reward.model.PlatformEpochEntity;
import com.blockwin.protocol_api.reward.model.PlatformEpochId;
import com.blockwin.protocol_api.reward.model.dto.DepositRewardRequest;
import com.blockwin.protocol_api.reward.model.dto.MerkleProofResponse;
import com.blockwin.protocol_api.reward.repository.EpochParticipationRepository;
import com.blockwin.protocol_api.reward.repository.PlatformEpochRepository;
import com.blockwin.protocol_api.validator.model.enums.ChainName;
import com.blockwin.protocol_api.validator.service.ValidatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock private PlatformRepository platformRepository;
    @Mock private PlatformEpochRepository platformEpochRepository;
    @Mock private EpochParticipationRepository epochParticipationRepository;
    @Mock private ValidatorService validatorService;
    @Mock private MultiChainService multiChainService;
    @Mock private BlockchainConfig blockchainConfig;
    @Mock private TransactionManagementService transactionManagementService;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private RewardService rewardService;

    private UUID platformId;
    private long epochId;
    private PlatformEntity platform;

    @BeforeEach
    void setUp() {
        platformId = UUID.randomUUID();
        epochId = 20260401L;
        platform = PlatformEntity.builder()
                .id(platformId)
                .url("example.com")
                .checkIntervalSeconds(60L)
                .build();
    }

    @Test
    void closeEpoch_platformNotFound_throwsIllegalArgumentException() {
        when(platformRepository.findById(platformId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> rewardService.closeEpoch(platformId, epochId));
    }

    @Test
    void closeEpoch_epochNotFound_throwsIllegalStateException() {
        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> rewardService.closeEpoch(platformId, epochId));
    }

    @Test
    void closeEpoch_alreadyClosed_throwsIllegalStateException() {
        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(closedEpoch()));

        assertThrows(IllegalStateException.class,
                () -> rewardService.closeEpoch(platformId, epochId));
    }

    @Test
    void closeEpoch_noParticipations_throwsIllegalStateException() {
        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(openEpoch(BigInteger.valueOf(1_000_000))));
        when(epochParticipationRepository.findByIdPlatformUrlAndIdEpochId("example.com", epochId))
                .thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> rewardService.closeEpoch(platformId, epochId));
    }

    @Test
    void closeEpoch_noValidatorsWithChainAddress_throwsIllegalStateException() {
        UUID validatorId = UUID.randomUUID();
        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(openEpoch(BigInteger.valueOf(1_000_000))));
        when(epochParticipationRepository.findByIdPlatformUrlAndIdEpochId("example.com", epochId))
                .thenReturn(List.of(participation(validatorId, 10L)));
        when(validatorService.getEthAddress(validatorId, ChainName.ETHEREUM))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> rewardService.closeEpoch(platformId, epochId));
    }

    @Test
    void closeEpoch_allValidatorsHaveZeroReputation_throwsIllegalStateException() {
        UUID validatorId = UUID.randomUUID();
        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(openEpoch(BigInteger.valueOf(1_000_000))));
        when(epochParticipationRepository.findByIdPlatformUrlAndIdEpochId("example.com", epochId))
                .thenReturn(List.of(participation(validatorId, 10L)));
        when(validatorService.getEthAddress(validatorId, ChainName.ETHEREUM))
                .thenReturn(Optional.of("0xaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAa"));
        when(validatorService.getReputationBps(validatorId)).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> rewardService.closeEpoch(platformId, epochId));
    }

    @Test
    void closeEpoch_happyPath_distributesRewardsProportionalToReputationAndRounds() {
        UUID v1 = UUID.randomUUID();
        UUID v2 = UUID.randomUUID();
        String a1 = "0xaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAa";
        String a2 = "0xbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBb";

        // v1: 10 rounds × 6000 bps = 60 000 share
        // v2:  5 rounds × 4000 bps = 20 000 share → total 80 000
        // v1 reward = 1 000 000 × 60 000 / 80 000 = 750 000
        // v2 reward = 1 000 000 × 20 000 / 80 000 = 250 000
        EpochParticipationEntity p1 = participation(v1, 10L);
        EpochParticipationEntity p2 = participation(v2, 5L);
        PlatformEpochEntity epoch   = openEpoch(BigInteger.valueOf(1_000_000));

        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(epoch));
        when(epochParticipationRepository.findByIdPlatformUrlAndIdEpochId("example.com", epochId))
                .thenReturn(List.of(p1, p2));
        when(validatorService.getEthAddress(v1, ChainName.ETHEREUM)).thenReturn(Optional.of(a1));
        when(validatorService.getEthAddress(v2, ChainName.ETHEREUM)).thenReturn(Optional.of(a2));
        when(validatorService.getReputationBps(v1)).thenReturn(6000);
        when(validatorService.getReputationBps(v2)).thenReturn(4000);

        rewardService.closeEpoch(platformId, epochId);

        assertEquals(BigInteger.valueOf(750_000), p1.getRewardAmount());
        assertEquals(BigInteger.valueOf(250_000), p2.getRewardAmount());
        assertNotNull(epoch.getMerkleRoot());
        assertTrue(epoch.getMerkleRoot().startsWith("0x"));
        assertEquals(66, epoch.getMerkleRoot().length()); // "0x" + 64 hex chars
        assertNotNull(epoch.getClosedAt());
        assertEquals(ChainName.ETHEREUM, epoch.getChainName());
        verify(platformEpochRepository).save(epoch);
    }

    @Test
    void closeEpoch_validatorWithoutChainAddressIsSkipped_remainingValidatorReceivesFullPot() {
        UUID withAddress = UUID.randomUUID();
        UUID withoutAddress = UUID.randomUUID();
        String addr = "0xaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAa";
        BigInteger pot = BigInteger.valueOf(1000);

        EpochParticipationEntity p1 = participation(withAddress, 5L);

        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(openEpoch(pot)));
        when(epochParticipationRepository.findByIdPlatformUrlAndIdEpochId("example.com", epochId))
                .thenReturn(List.of(p1, participation(withoutAddress, 10L)));
        when(validatorService.getEthAddress(withAddress, ChainName.ETHEREUM))
                .thenReturn(Optional.of(addr));
        when(validatorService.getEthAddress(withoutAddress, ChainName.ETHEREUM))
                .thenReturn(Optional.empty());
        when(validatorService.getReputationBps(withAddress)).thenReturn(5000);

        rewardService.closeEpoch(platformId, epochId);

        // withAddress has the only share, so it receives the full pot
        assertEquals(pot, p1.getRewardAmount());
    }

    @Test
    void closeEpoch_equalReputationAndRounds_splitsPotEvenly() {
        UUID v1 = UUID.randomUUID();
        UUID v2 = UUID.randomUUID();
        String a1 = "0xaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAa";
        String a2 = "0xbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBb";

        EpochParticipationEntity p1 = participation(v1, 10L);
        EpochParticipationEntity p2 = participation(v2, 10L);

        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(openEpoch(BigInteger.valueOf(1000))));
        when(epochParticipationRepository.findByIdPlatformUrlAndIdEpochId("example.com", epochId))
                .thenReturn(List.of(p1, p2));
        when(validatorService.getEthAddress(v1, ChainName.ETHEREUM)).thenReturn(Optional.of(a1));
        when(validatorService.getEthAddress(v2, ChainName.ETHEREUM)).thenReturn(Optional.of(a2));
        when(validatorService.getReputationBps(v1)).thenReturn(5000);
        when(validatorService.getReputationBps(v2)).thenReturn(5000);

        rewardService.closeEpoch(platformId, epochId);

        assertEquals(BigInteger.valueOf(500), p1.getRewardAmount());
        assertEquals(BigInteger.valueOf(500), p2.getRewardAmount());
    }

    @Test
    void getMerkleProof_platformNotFound_throwsIllegalArgumentException() {
        when(platformRepository.findById(platformId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> rewardService.getMerkleProof(platformId, epochId, "0xabc"));
    }

    @Test
    void getMerkleProof_epochNotFound_throwsIllegalStateException() {
        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> rewardService.getMerkleProof(platformId, epochId, "0xabc"));
    }

    @Test
    void getMerkleProof_epochNotClosed_throwsIllegalStateException() {
        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(openEpoch(BigInteger.valueOf(1000))));

        assertThrows(IllegalStateException.class,
                () -> rewardService.getMerkleProof(platformId, epochId, "0xabc"));
    }

    @Test
    void getMerkleProof_validatorNotInEpoch_throwsIllegalArgumentException() {
        UUID otherValidator = UUID.randomUUID();
        String unknownAddr = "0xDdDdDdDdDdDdDdDdDdDdDdDdDdDdDdDdDdDdDd";

        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(closedEpoch()));
        when(epochParticipationRepository.findByIdPlatformUrlAndIdEpochId("example.com", epochId))
                .thenReturn(List.of(participationWithReward(
                        otherValidator, 10L, BigInteger.valueOf(500))));
        when(validatorService.getEthAddress(otherValidator, ChainName.ETHEREUM))
                .thenReturn(Optional.of("0xaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAa"));

        assertThrows(IllegalArgumentException.class,
                () -> rewardService.getMerkleProof(platformId, epochId, unknownAddr));
    }


    @Test
    void getMerkleProof_happyPath_returnsResponseWithCorrectFields() {
        UUID validatorId = UUID.randomUUID();
        String validatorAddress = "0xaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAaAa";
        BigInteger reward = BigInteger.valueOf(750_000);

        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(closedEpoch()));
        when(epochParticipationRepository.findByIdPlatformUrlAndIdEpochId("example.com", epochId))
                .thenReturn(List.of(participationWithReward(validatorId, 10L, reward)));
        when(validatorService.getEthAddress(validatorId, ChainName.ETHEREUM))
                .thenReturn(Optional.of(validatorAddress));

        MerkleProofResponse response = rewardService.getMerkleProof(platformId, epochId, validatorAddress);

        assertEquals(epochId, response.epochId());
        assertEquals(validatorAddress, response.validatorAddress());
        assertEquals(reward.toString(), response.rewardAmount());
        assertEquals(closedEpoch().getMerkleRoot(), response.merkleRoot());
        assertNotNull(response.proof());
        assertTrue(response.websiteId().startsWith("0x"));
    }

    @Test
    void getMerkleProof_addressLookupIsCaseInsensitive() {
        UUID validatorId = UUID.randomUUID();
        String storedAddress = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 40 lowercase hex chars
        String queriedAddress = "0xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // same value, uppercase
        BigInteger reward = BigInteger.valueOf(500);

        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(closedEpoch()));
        when(epochParticipationRepository.findByIdPlatformUrlAndIdEpochId("example.com", epochId))
                .thenReturn(List.of(participationWithReward(validatorId, 5L, reward)));
        when(validatorService.getEthAddress(validatorId, ChainName.ETHEREUM))
                .thenReturn(Optional.of(storedAddress));

        // Should not throw — both addresses normalise to the same lowercase key
        assertDoesNotThrow(() -> rewardService.getMerkleProof(platformId, epochId, queriedAddress));
    }


    @Test
    void publishRoot_epochNotClosed_throwsIllegalStateException() {
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(openEpoch(BigInteger.valueOf(1000))));

        assertThrows(IllegalStateException.class,
                () -> rewardService.publishRoot(platformId, epochId, "ethereum"));
    }

    @Test
    void publishRoot_alreadyPublished_throwsIllegalStateException() {
        PlatformEpochEntity published = PlatformEpochEntity.builder()
                .id(new PlatformEpochId(platformId, epochId))
                .merkleRoot("0x" + "aa".repeat(32))
                .published(true)
                .build();
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(published));

        assertThrows(IllegalStateException.class,
                () -> rewardService.publishRoot(platformId, epochId, "ethereum"));
    }


    @Test
    void verifyRewardDeposit_alreadyActive_throwsIllegalStateExceptionWithoutCallingBlockchain() {
        DepositRewardRequest request = depositRequest(BigInteger.valueOf(1_000_000), 30);
        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.existsByIdPlatformIdAndValidationEndTimestampAfter(
                eq(platformId), any(Instant.class))).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> rewardService.verifyRewardDeposit(platformId, request));
        verifyNoInteractions(transactionManagementService);
    }

    @Test
    void verifyRewardDeposit_happyPath_savesEntityWithCorrectFields() {
        BigInteger amount = BigInteger.valueOf(5_000_000);
        int validationDays = 30;
        DepositRewardRequest request = depositRequest(amount, validationDays);
        long expectedEpochId = EpochService.toEpochId(Instant.now().plus(validationDays, ChronoUnit.DAYS));

        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.existsByIdPlatformIdAndValidationEndTimestampAfter(
                eq(platformId), any(Instant.class))).thenReturn(false);

        rewardService.verifyRewardDeposit(platformId, request);

        ArgumentCaptor<PlatformEpochEntity> captor = ArgumentCaptor.forClass(PlatformEpochEntity.class);
        verify(platformEpochRepository).save(captor.capture());
        PlatformEpochEntity saved = captor.getValue();
        assertEquals(new PlatformEpochId(platformId, expectedEpochId), saved.getId());
        assertEquals(amount, saved.getRewardPot());
        assertEquals(request.txHash(), saved.getDepositTxHash());
        assertFalse(saved.isPublished());
        assertNull(saved.getMerkleRoot());
        assertEquals(ChainName.ETHEREUM, saved.getChainName());
        assertNull(saved.getClosedAt());
    }

    @Test
    void verifyRewardDeposit_happyPath_publishesCachePlatformEventWithCorrectFields() {
        int validationDays = 30;
        DepositRewardRequest request = depositRequest(BigInteger.valueOf(1_000_000), validationDays);

        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.existsByIdPlatformIdAndValidationEndTimestampAfter(
                eq(platformId), any(Instant.class))).thenReturn(false);

        rewardService.verifyRewardDeposit(platformId, request);

        ArgumentCaptor<CachePlatformEvent> captor = ArgumentCaptor.forClass(CachePlatformEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        CachePlatformEvent event = captor.getValue();
        assertEquals(platformId, event.getPlatformId());
        assertEquals("example.com", event.getPlatformURL());
        assertEquals(platform.getCheckIntervalSeconds(), event.getCheckIntervalSeconds());
        // Validation window ends approximately validationDays from now
        long expectedEndEpoch = Instant.now().plus(validationDays, ChronoUnit.DAYS).getEpochSecond();
        long actualEndEpoch = event.getValidationEndTimestamp().getEpochSecond();
        assertTrue(Math.abs(actualEndEpoch - expectedEndEpoch) < 5, "End timestamp should be ~30 days from now");
    }

    private DepositRewardRequest depositRequest(BigInteger amount, int validationDays) {
        return new DepositRewardRequest(
                "0x" + "d".repeat(64),
                "ethereum",
                "0xOwner" + "0".repeat(34),
                "example.com",
                amount,
                validationDays
        );
    }

    private PlatformEpochEntity openEpoch(BigInteger rewardPot) {
        return PlatformEpochEntity.builder()
                .id(new PlatformEpochId(platformId, epochId))
                .rewardPot(rewardPot)
                .chainName(ChainName.ETHEREUM)
                .build();
    }

    private PlatformEpochEntity closedEpoch() {
        return PlatformEpochEntity.builder()
                .id(new PlatformEpochId(platformId, epochId))
                .rewardPot(BigInteger.valueOf(1_000_000))
                .merkleRoot("0x" + "ab".repeat(32))
                .chainName(ChainName.ETHEREUM)
                .build();
    }

    private EpochParticipationEntity participation(UUID validatorId, long rounds) {
        return EpochParticipationEntity.builder()
                .id(new EpochParticipationId(validatorId, "example.com", epochId))
                .roundsParticipated(rounds)
                .build();
    }

    private EpochParticipationEntity participationWithReward(
            UUID validatorId, long rounds, BigInteger reward
    ) {
        EpochParticipationEntity entity = participation(validatorId, rounds);
        entity.setRewardAmount(reward);
        return entity;
    }
}