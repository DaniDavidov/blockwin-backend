package com.blockwin.protocol_api.reward.service;

import com.blockwin.protocol_api.blockchain.config.BlockchainConfig;
import com.blockwin.protocol_api.blockchain.service.MultiChainService;
import com.blockwin.protocol_api.blockchain.service.SignatureService;
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
import com.blockwin.protocol_api.reward.model.enums.ValidationPeriodUnit;
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
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    @Mock private TransactionManagementService transactionManagementService;
    @Mock private SignatureService signatureService;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private RewardService rewardService;

    private static final String OWNER_ADDRESS = "0x" + "a".repeat(40);

    private UUID platformId;
    private long epochId;
    private PlatformEntity platform;

    @BeforeEach
    void setUp() {
        platformId = UUID.randomUUID();
        epochId = 202604010000L;
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
    void computeLeaf_matchesContractClaimRewardEncoding() {
        // Expected value computed independently with the contract's formula for a real claim on Anvil:
        // keccak256(bytes.concat(keccak256(abi.encode(msg.sender, platformId, epoch, amount))))
        byte[] platformId32 = Numeric.hexStringToByteArray(
                "0x75181c38e50441508bf851f0d69b01cd00000000000000000000000000000000");

        byte[] leaf = rewardService.computeLeaf(
                platformId32, 202610211108L, "0x90F79bf6EB2c4f870365E785982E1f101E93b906", BigInteger.valueOf(20_000_000));

        assertEquals("0x7231b3199e6fd8fc2a87cfd73641e6fae53c494f2e4ef7d75343a75b7bcd67f5", Numeric.toHexString(leaf));
    }

    @Test
    void closeEpochThenGetMerkleProof_everyProofVerifiesLikeTheContract() {
        Map<UUID, String> addresses = new LinkedHashMap<>();
        addresses.put(UUID.randomUUID(), "0x70997970C51812dc3A010C7d01b50e0d17dc79C8");
        addresses.put(UUID.randomUUID(), "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC");
        addresses.put(UUID.randomUUID(), "0x90F79bf6EB2c4f870365E785982E1f101E93b906");
        List<EpochParticipationEntity> participations = new ArrayList<>();
        long rounds = 10;
        for (Map.Entry<UUID, String> e : addresses.entrySet()) {
            participations.add(participation(e.getKey(), rounds++));
            when(validatorService.getEthAddress(e.getKey(), ChainName.ETHEREUM)).thenReturn(Optional.of(e.getValue()));
            when(validatorService.getReputationBps(e.getKey())).thenReturn(5000);
        }
        PlatformEpochEntity epoch = openEpoch(BigInteger.valueOf(1_000_000));
        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId))).thenReturn(Optional.of(epoch));
        when(epochParticipationRepository.findByIdPlatformUrlAndIdEpochId("example.com", epochId)).thenReturn(participations);

        rewardService.closeEpoch(platformId, epochId);

        for (String address : addresses.values()) {
            MerkleProofResponse response = rewardService.getMerkleProof(platformId, epochId, address);
            byte[] node = contractLeaf(address, response.websiteId(), epochId, new BigInteger(response.rewardAmount()));
            for (String sibling : response.proof()) {
                node = hashSortedPair(node, Numeric.hexStringToByteArray(sibling));
            }
            assertEquals(epoch.getMerkleRoot(), Numeric.toHexString(node), "proof for " + address + " must reach the root");
        }
    }

    /** Mirrors {@code claimReward}'s leaf, using web3j's ABI encoder rather than the production code. */
    private static byte[] contractLeaf(String address, String platformId32, long epoch, BigInteger amount) {
        String encoded = TypeEncoder.encode(new Address(address))
                + TypeEncoder.encode(new Bytes32(Numeric.hexStringToByteArray(platformId32)))
                + TypeEncoder.encode(new Uint256(BigInteger.valueOf(epoch)))
                + TypeEncoder.encode(new Uint256(amount));
        return Hash.sha3(Hash.sha3(Numeric.hexStringToByteArray(encoded)));
    }

    /** OpenZeppelin {@code MerkleProof} commutative pair hash. */
    private static byte[] hashSortedPair(byte[] a, byte[] b) {
        byte[] first = Arrays.compareUnsigned(a, b) <= 0 ? a : b;
        byte[] second = first == a ? b : a;
        byte[] concat = new byte[64];
        System.arraycopy(first, 0, concat, 0, 32);
        System.arraycopy(second, 0, concat, 32, 32);
        return Hash.sha3(concat);
    }

    @Test
    void getMerkleProof_addressLookupIsCaseInsensitive() {
        UUID validatorId = UUID.randomUUID();
        String storedAddress = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String queriedAddress = "0xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        BigInteger reward = BigInteger.valueOf(500);

        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.findById(new PlatformEpochId(platformId, epochId)))
                .thenReturn(Optional.of(closedEpoch()));
        when(epochParticipationRepository.findByIdPlatformUrlAndIdEpochId("example.com", epochId))
                .thenReturn(List.of(participationWithReward(validatorId, 5L, reward)));
        when(validatorService.getEthAddress(validatorId, ChainName.ETHEREUM))
                .thenReturn(Optional.of(storedAddress));

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
        DepositRewardRequest request = depositRequest(30, ValidationPeriodUnit.DAYS);
        when(signatureService.recoverSignerAddressUnchecked(any(), any())).thenReturn(OWNER_ADDRESS);
        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.existsByIdPlatformIdAndValidationEndTimestampAfter(
                eq(platformId), any(Instant.class))).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> rewardService.verifyRewardDeposit(platformId, request));
        verifyNoInteractions(transactionManagementService);
    }

    @Test
    void verifyRewardDeposit_happyPath_savesEntityWithCorrectFields() {
        BigInteger amount = BigInteger.valueOf(5_000_000);
        DepositRewardRequest request = depositRequest(30, ValidationPeriodUnit.DAYS);

        when(signatureService.recoverSignerAddressUnchecked(any(), any())).thenReturn(OWNER_ADDRESS);
        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.existsByIdPlatformIdAndValidationEndTimestampAfter(
                eq(platformId), any(Instant.class))).thenReturn(false);
        when(transactionManagementService.validateRewardDeposit(eq(platformId), any())).thenReturn(amount);

        rewardService.verifyRewardDeposit(platformId, request);

        ArgumentCaptor<PlatformEpochEntity> captor = ArgumentCaptor.forClass(PlatformEpochEntity.class);
        verify(platformEpochRepository).save(captor.capture());
        PlatformEpochEntity saved = captor.getValue();
        assertEquals(new PlatformEpochId(platformId, EpochService.toEpochId(saved.getValidationEndTimestamp())), saved.getId());
        assertEquals(Duration.ofDays(30),
                Duration.between(saved.getValidationStartTimestamp(), saved.getValidationEndTimestamp()));
        assertEquals(amount, saved.getRewardPot());
        assertEquals(request.txHash(), saved.getDepositTxHash());
        assertFalse(saved.isPublished());
        assertNull(saved.getMerkleRoot());
        assertEquals(ChainName.ETHEREUM, saved.getChainName());
        assertNull(saved.getClosedAt());
    }

    @Test
    void verifyRewardDeposit_happyPath_publishesCachePlatformEventWithCorrectFields() {
        DepositRewardRequest request = depositRequest(15, ValidationPeriodUnit.MINUTES);

        when(signatureService.recoverSignerAddressUnchecked(any(), any())).thenReturn(OWNER_ADDRESS);
        when(platformRepository.findById(platformId)).thenReturn(Optional.of(platform));
        when(platformEpochRepository.existsByIdPlatformIdAndValidationEndTimestampAfter(
                eq(platformId), any(Instant.class))).thenReturn(false);
        when(transactionManagementService.validateRewardDeposit(eq(platformId), any()))
                .thenReturn(BigInteger.valueOf(1_000_000));

        rewardService.verifyRewardDeposit(platformId, request);

        ArgumentCaptor<CachePlatformEvent> captor = ArgumentCaptor.forClass(CachePlatformEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        CachePlatformEvent event = captor.getValue();
        assertEquals(platformId, event.getPlatformId());
        assertEquals("example.com", event.getPlatformURL());
        assertEquals(platform.getCheckIntervalSeconds(), event.getCheckIntervalSeconds());
        assertEquals(Duration.ofMinutes(15),
                Duration.between(event.getValidationStartTimestamp(), event.getValidationEndTimestamp()));
        assertEquals(EpochService.toEpochId(event.getValidationEndTimestamp()), event.getEpochId());
    }

    private DepositRewardRequest depositRequest(long validationPeriod, ValidationPeriodUnit unit) {
        return new DepositRewardRequest(
                "0x" + "d".repeat(64),
                "ethereum",
                OWNER_ADDRESS,
                validationPeriod,
                unit,
                "0x" + "e".repeat(130)
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