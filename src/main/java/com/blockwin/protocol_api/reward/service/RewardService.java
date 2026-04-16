package com.blockwin.protocol_api.reward.service;

import com.blockwin.protocol_api.blockchain.config.BlockchainConfig;
import com.blockwin.protocol_api.blockchain.model.dto.ChainConfig;
import com.blockwin.protocol_api.blockchain.service.MultiChainService;
import com.blockwin.protocol_api.blockchain.service.TransactionManagementService;
import com.blockwin.protocol_api.platform.event.CachePlatformEvent;
import com.blockwin.protocol_api.platform.model.PlatformEntity;
import com.blockwin.protocol_api.platform.repository.PlatformRepository;
import com.blockwin.protocol_api.reward.model.EpochParticipationEntity;
import com.blockwin.protocol_api.reward.model.PlatformEpochEntity;
import com.blockwin.protocol_api.reward.model.PlatformEpochId;
import com.blockwin.protocol_api.reward.model.dto.DepositRewardRequest;
import com.blockwin.protocol_api.reward.model.dto.MerkleProofResponse;
import com.blockwin.protocol_api.reward.repository.EpochParticipationRepository;
import com.blockwin.protocol_api.reward.repository.PlatformEpochRepository;
import com.blockwin.protocol_api.reward.util.MerkleTree;
import com.blockwin.protocol_api.validator.model.enums.ChainName;
import com.blockwin.protocol_api.validator.service.ValidatorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.utils.Numeric;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Core reward mechanism service.
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li><b>depositReward</b> — Platform owner declares the reward pot for an epoch.</li>
 *   <li><b>closeEpoch</b>   — Admin closes the epoch: computes each validator's share
 *       ({@code roundsParticipated / totalRounds * rewardPot}), builds the Merkle tree,
 *       and persists the root. No on-chain write yet.</li>
 *   <li><b>publishRoot</b>  — Admin submits the root to the reward smart contract
 *       ({@code publishRoot(bytes32,uint256,bytes32)}).</li>
 *   <li><b>getMerkleProof</b> — Validator requests the leaf data and Merkle proof
 *       needed to call {@code claim()} on the contract.</li>
 * </ol>
 *
 * <h2>Leaf encoding</h2>
 * {@code keccak256(abi.encodePacked(bytes32 websiteId, uint256 epochId, address validatorAddress, uint256 rewardAmount))}
 * — 116 bytes packed, matching the contract's claim verification.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RewardService {

    private static final BigInteger GAS_LIMIT_PUBLISH = BigInteger.valueOf(120_000L);

    private final PlatformRepository platformRepository;
    private final PlatformEpochRepository platformEpochRepository;
    private final EpochParticipationRepository epochParticipationRepository;
    private final ValidatorService validatorService;
    private final MultiChainService multiChainService;
    private final BlockchainConfig blockchainConfig;
    private final TransactionManagementService transactionManagementService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Records a reward pot for the given platform epoch by verifying the platform owner's
     * on-chain {@code depositReward()} transaction.
     *
     * <p>Delegates receipt inspection and event decoding to
     * {@link TransactionManagementService#validateRewardDeposit} — no blockchain logic lives here.
     * The decoded amount becomes the epoch's {@code rewardPot}.
     *
     * @throws IllegalStateException    if a reward pot is already registered for this epoch
     * @throws IllegalArgumentException if the on-chain event data does not match the request
     */
    @Transactional
    public void verifyRewardDeposit(UUID platformId, DepositRewardRequest request) {
        PlatformEntity platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new IllegalArgumentException("Platform not found: " + platformId));

        Instant validationStartTimestamp = Instant.now();
        Instant validationEndTimestamp = validationStartTimestamp.plus(request.validationDays(), ChronoUnit.DAYS);
        long epochId = EpochService.toEpochId(validationEndTimestamp);
        PlatformEpochId epochKey = new PlatformEpochId(platformId, epochId);

        if (platformEpochRepository.existsByIdPlatformIdAndValidationEndTimestampAfter(platformId, validationStartTimestamp)) {
            throw new IllegalStateException(
                    "Reward pot already deposited for platform " + platformId + " epoch " + epochKey);
        }

        transactionManagementService.validateRewardDeposit(request);

        PlatformEpochEntity entity = PlatformEpochEntity.builder()
                .id(epochKey)
                .depositTxHash(request.txHash())
                .rewardPot(request.amount())
                .validationEndTimestamp(validationEndTimestamp)
                .validationStartTimestamp(validationStartTimestamp)
                .chainName(ChainName.valueOf(request.chainName().toUpperCase()))
                .published(false)
                .build();
        platformEpochRepository.save(entity);

        CachePlatformEvent event = new CachePlatformEvent(
                this,
                platformId,
                platform.getUrl(),
                platform.getCheckIntervalSeconds(),
                validationStartTimestamp,
                validationEndTimestamp
        );
        applicationEventPublisher.publishEvent(event);

        log.info("Reward pot deposited: platformId={} epochId={} amount={} txHash={}",
                platformId, epochId, request.amount(), request.txHash());
    }

    /**
     * Closes the epoch: computes reputation-weighted reward shares, builds the Merkle tree,
     * and stores the root in the database. No on-chain write happens here.
     */
    @Transactional
    public void closeEpoch(UUID platformId, long epochId) {
        PlatformEntity platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new IllegalArgumentException("Platform not found: " + platformId));

        PlatformEpochId epochKey = new PlatformEpochId(platformId, epochId);
        PlatformEpochEntity platformEpoch = platformEpochRepository.findById(epochKey)
                .orElseThrow(() -> new IllegalStateException(
                        "No reward pot configured for platform " + platformId + " epoch " + epochId));

        if (platformEpoch.getMerkleRoot() != null) {
            throw new IllegalStateException("Epoch already closed. Root: " + platformEpoch.getMerkleRoot());
        }

        List<EpochParticipationEntity> participations =
                epochParticipationRepository.findByIdPlatformUrlAndIdEpochId(platform.getUrl(), epochId);

        if (participations.isEmpty()) {
            throw new IllegalStateException(
                    "No validator participation recorded for platform " + platformId + " epoch " + epochId);
        }

        BigInteger rewardPot   = platformEpoch.getRewardPot();
        byte[]     websiteId32 = platformIdToBytes32(platformId);

        // share_i   = roundsParticipated_i × reputationBps_i
        // reward_i  = pot × share_i / Σ share_j
        //
        // MAX_BPS cancels in numerator and denominator so we never divide by it.
        // Validators with 0 reputation get a 0 share; the pot is distributed
        // entirely among those with a positive share — no remainder stays in limbo.
        record ValidatorSlice(EpochParticipationEntity participation, String ethAddress, long weightedShare) {}

        List<ValidatorSlice> slices = new ArrayList<>();
        long totalWeightedShares = 0;

        ChainName chainName = platformEpoch.getChainName();
        for (EpochParticipationEntity participation : participations) {
            UUID validatorId = participation.getId().getValidatorId();
            Optional<String> address = resolveChainAddress(validatorId, chainName);
            if (address.isEmpty()) {
                log.warn("Skipping validator {} — no address found on chain {}", validatorId, chainName);
                continue;
            }
            int reputationBps = validatorService.getReputationBps(validatorId);
            long weightedShare = participation.getRoundsParticipated() * reputationBps;
            slices.add(new ValidatorSlice(participation, address.get(), weightedShare));
            totalWeightedShares += weightedShare;
        }

        if (slices.isEmpty()) {
            throw new IllegalStateException(
                    "No validator with an address on chain " + chainName + " participated");
        }

        List<byte[]> leaves = new ArrayList<>();
        for (ValidatorSlice slice : slices) {
            BigInteger rewardAmount = totalWeightedShares > 0
                    ? rewardPot.multiply(BigInteger.valueOf(slice.weightedShare()))
                               .divide(BigInteger.valueOf(totalWeightedShares))
                    : BigInteger.ZERO;

            slice.participation().setRewardAmount(rewardAmount);
            epochParticipationRepository.save(slice.participation());

            // Only include in the Merkle tree if there is actually a reward to claim.
            if (rewardAmount.compareTo(BigInteger.ZERO) > 0) {
                leaves.add(computeLeaf(websiteId32, epochId, slice.ethAddress(), rewardAmount));
            }
        }

        if (leaves.isEmpty()) {
            throw new IllegalStateException(
                    "All participating validators have 0 reputation — no rewards to distribute");
        }

        MerkleTree tree = new MerkleTree(leaves);
        String merkleRoot = tree.getRootHex();

        platformEpoch.setChainName(chainName);
        platformEpoch.setMerkleRoot(merkleRoot);
        platformEpoch.setClosedAt(Instant.now());
        platformEpochRepository.save(platformEpoch);

        log.info("Epoch closed: platformId={} epochId={} validators={} root={}",
                platformId, epochId, leaves.size(), merkleRoot);
    }

    /**
     * Submits the stored Merkle root to the reward smart contract by sending a signed
     * {@code publishRoot(bytes32 websiteId, uint256 epochId, bytes32 merkleRoot)} transaction.
     *
     * @param chainName logical chain name (e.g. "ethereum")
     * @return transaction hash
     */
    @Transactional
    public String publishRoot(UUID platformId, long epochId, String chainName) throws Exception {
        PlatformEpochId epochKey = new PlatformEpochId(platformId, epochId);
        PlatformEpochEntity platformEpoch = platformEpochRepository.findById(epochKey)
                .orElseThrow(() -> new IllegalStateException("Epoch not found"));

        if (platformEpoch.getMerkleRoot() == null) {
            throw new IllegalStateException("Epoch not closed yet — call closeEpoch() first");
        }
        if (platformEpoch.isPublished()) {
            throw new IllegalStateException("Root already published on-chain for this epoch");
        }

        ChainConfig chainConfig = blockchainConfig.getChain(chainName);
        Web3j web3j = multiChainService.getWeb3j(chainName);
        Credentials credentials = Credentials.create(chainConfig.getApiPrivateKey());

        byte[] websiteIdBytes = platformIdToBytes32(platformId);
        byte[] merkleRootBytes = Numeric.hexStringToByteArray(platformEpoch.getMerkleRoot());

        // ABI-encode: publishRoot(bytes32, uint256, bytes32)
        Function function = new Function(
                "publishRoot",
                Arrays.asList(
                        new Bytes32(websiteIdBytes),
                        new Uint256(BigInteger.valueOf(epochId)),
                        new Bytes32(merkleRootBytes)
                ),
                Collections.emptyList()
        );
        String data = FunctionEncoder.encode(function);

        BigInteger nonce = web3j
                .ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                .send()
                .getTransactionCount();

        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

        RawTransaction rawTx = RawTransaction.createTransaction(
                nonce, gasPrice, GAS_LIMIT_PUBLISH,
                chainConfig.getRewardContract(),
                BigInteger.ZERO,
                data
        );

        byte[] signed = TransactionEncoder.signMessage(rawTx, chainConfig.getChainId().longValue(), credentials);
        String txHash = web3j.ethSendRawTransaction(Numeric.toHexString(signed))
                .send()
                .getTransactionHash();

        platformEpoch.setPublished(true);
        platformEpochRepository.save(platformEpoch);

        log.info("Root published on-chain: platformId={} epochId={} chain={} txHash={}",
                platformId, epochId, chainName, txHash);
        return txHash;
    }

    /**
     * Reconstructs the Merkle tree from persisted participation data and returns the
     * leaf inputs and proof path for the given validator address.
     *
     * <p>The validator passes this data directly to the contract's
     * {@code claim(bytes32 websiteId, uint256 epochId, uint256 rewardAmount, bytes32[] proof)}.
     */
    public MerkleProofResponse getMerkleProof(UUID platformId, long epochId, String validatorAddress) {
        PlatformEntity platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new IllegalArgumentException("Platform not found: " + platformId));

        PlatformEpochId epochKey = new PlatformEpochId(platformId, epochId);
        PlatformEpochEntity platformEpoch = platformEpochRepository.findById(epochKey)
                .orElseThrow(() -> new IllegalStateException("Epoch not found or reward pot not set"));

        if (platformEpoch.getMerkleRoot() == null) {
            throw new IllegalStateException("Epoch not closed yet — proof unavailable");
        }

        List<EpochParticipationEntity> participations =
                epochParticipationRepository.findByIdPlatformUrlAndIdEpochId(platform.getUrl(), epochId);

        byte[] websiteId32 = platformIdToBytes32(platformId);
        List<byte[]> leaves = new ArrayList<>();
        Map<String, BigInteger> rewardByAddr = new HashMap<>();

        ChainName chain = platformEpoch.getChainName();
        for (EpochParticipationEntity p : participations) {
            if (p.getRewardAmount() == null) continue;
            Optional<String> addrOpt = resolveChainAddress(p.getId().getValidatorId(), chain);
            if (addrOpt.isEmpty()) continue;
            String ethAddr = addrOpt.get();

            byte[] leaf = computeLeaf(websiteId32, epochId, ethAddr, p.getRewardAmount());
            leaves.add(leaf);
            rewardByAddr.put(normalise(ethAddr), p.getRewardAmount());
        }

        BigInteger rewardAmount = rewardByAddr.get(normalise(validatorAddress));
        if (rewardAmount == null) {
            throw new IllegalArgumentException(
                    "Validator " + validatorAddress + " did not participate in epoch " + epochId);
        }

        byte[] targetLeaf = computeLeaf(websiteId32, epochId, validatorAddress, rewardAmount);
        MerkleTree tree = new MerkleTree(leaves);
        List<String> proof = tree.getProofHex(targetLeaf);

        return new MerkleProofResponse(
                "0x" + MerkleTree.toHex(websiteId32),
                epochId,
                validatorAddress,
                rewardAmount.toString(),
                proof,
                platformEpoch.getMerkleRoot()
        );
    }

    /**
     * Builds a Merkle leaf:
     * {@code keccak256(abi.encodePacked(bytes32 websiteId, uint256 epochId, address validatorAddress, uint256 rewardAmount))}
     *
     * <p>Packed layout (116 bytes):
     * <pre>
     *   [0..31]  websiteId       bytes32
     *   [32..63] epochId         uint256 (big-endian)
     *   [64..83] validatorAddress address (20 bytes, no padding in packed mode)
     *   [84..115] rewardAmount   uint256 (big-endian)
     * </pre>
     */
    private byte[] computeLeaf(byte[] websiteId32, long epochId, String validatorAddress, BigInteger rewardAmount) {
        byte[] packed = new byte[116];

        // bytes32 websiteId
        System.arraycopy(websiteId32, 0, packed, 0, 32);

        // uint256 epochId
        byte[] epochBytes = toUint256(BigInteger.valueOf(epochId));
        System.arraycopy(epochBytes, 0, packed, 32, 32);

        // address validatorAddress (20 bytes, no padding)
        String cleanAddr = validatorAddress.startsWith("0x") || validatorAddress.startsWith("0X")
                             ? validatorAddress.substring(2) : validatorAddress;
        byte[] addrBytes = Numeric.hexStringToByteArray(cleanAddr);
        System.arraycopy(addrBytes, 0, packed, 64, 20);

        // uint256 rewardAmount
        byte[] rewardBytes = toUint256(rewardAmount);
        System.arraycopy(rewardBytes, 0, packed, 84, 32);

        return Hash.sha3(packed);
    }

    /**
     * Encodes a platform UUID as a Solidity {@code bytes32}: first 16 bytes are the UUID
     * (MSB then LSB), last 16 bytes are zero-padded.
     */
    private byte[] platformIdToBytes32(UUID platformId) {
        byte[] result = new byte[32];
        ByteBuffer.wrap(result)
                  .putLong(platformId.getMostSignificantBits())
                  .putLong(platformId.getLeastSignificantBits());
        return result;
    }

    /**
     * Right-aligns a {@link BigInteger} into a 32-byte big-endian array (uint256 encoding).
     */
    private byte[] toUint256(BigInteger value) {
        byte[] raw = value.toByteArray();
        byte[] result = new byte[32];
        if (raw.length >= 32) {
            // Strip potential sign byte; take the low 32 bytes
            System.arraycopy(raw, raw.length - 32, result, 0, 32);
        } else {
            // Right-align (big-endian)
            System.arraycopy(raw, 0, result, 32 - raw.length, raw.length);
        }
        return result;
    }

    /**
     * Resolves a validator's on-chain address for the given chain.
     * Delegates to {@link ValidatorService} so the reward module never touches
     * validator repositories directly.
     */
    private Optional<String> resolveChainAddress(UUID validatorId, ChainName chain) {
        return validatorService.getEthAddress(validatorId, chain);
    }

    /** Normalises an address to lowercase without 0x prefix for map keying. */
    private String normalise(String address) {
        String stripped = address.startsWith("0x") || address.startsWith("0X")
                ? address.substring(2) : address;
        return stripped.toLowerCase();
    }
}
