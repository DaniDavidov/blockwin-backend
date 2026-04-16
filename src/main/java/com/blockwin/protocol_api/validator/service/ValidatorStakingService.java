package com.blockwin.protocol_api.validator.service;

import com.blockwin.protocol_api.blockchain.config.BlockchainConfig;
import com.blockwin.protocol_api.blockchain.model.Transaction;
import com.blockwin.protocol_api.blockchain.model.dto.ChainConfig;
import com.blockwin.protocol_api.blockchain.repository.TransactionRepository;
import com.blockwin.protocol_api.blockchain.service.SignatureService;
import com.blockwin.protocol_api.blockchain.service.TransactionManagementService;
import com.blockwin.protocol_api.validator.model.ValidatorChainEntity;
import com.blockwin.protocol_api.validator.model.ValidatorEntity;
import com.blockwin.protocol_api.validator.model.dto.StakeVerificationRequest;
import com.blockwin.protocol_api.validator.model.dto.StakeVerificationResponse;
import com.blockwin.protocol_api.validator.model.dto.UnstakeSignatureResponse;
import com.blockwin.protocol_api.validator.model.enums.ValidatorStatus;
import com.blockwin.protocol_api.validator.repository.ValidatorChainRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.UUID;

import static com.blockwin.protocol_api.common.utils.Constants.MAX_BPS;

@AllArgsConstructor
@Service
public class ValidatorStakingService {

    private final ValidatorService validatorService;
    private final ValidatorChainRepository validatorChainRepository;
    private final SignatureService signatureService;
    private final BlockchainConfig blockchainConfig;
    private final TransactionRepository transactionRepository;
    private final TransactionManagementService transactionManagementService;
    private final APIKeyService apiKeyService;

    public StakeVerificationResponse verifyStake(StakeVerificationRequest request) {
        String validatorAddress = signatureService.recoverSignerAddress(request.message(), request.signature());

        ValidatorChainEntity chainEntity = validatorChainRepository.findByPublicKey(validatorAddress)
                .orElseThrow(() -> new IllegalArgumentException("Validator not registered for address: " + validatorAddress));

        UUID validatorId = chainEntity.getValidator().getUuid();
        ValidatorStatus status = validatorService.getValidatorStatus(validatorId);

        if (status != ValidatorStatus.NO_STAKE) {
            throw new IllegalStateException("Stake cannot be verified in status: " + status);
        }

        String chainName = chainEntity.getChainName().name();
        transactionManagementService.validateStakingDeposit(request.txHash(), chainName, validatorAddress);

        validatorService.setValidatorStatus(validatorId, ValidatorStatus.INACTIVE);

        return new StakeVerificationResponse(apiKeyService.generateAPIKey(validatorId));
    }

    public UnstakeSignatureResponse generateUnstakeSignature(String message, String signature) {
        String validatorAddress = signatureService.recoverSignerAddress(message, signature);

        ValidatorChainEntity chainEntity = validatorChainRepository.findByPublicKey(validatorAddress)
                .orElseThrow(() -> new IllegalArgumentException("No validator registered for address: " + validatorAddress));

        UUID validatorId = chainEntity.getValidator().getUuid();

        ValidatorStatus status = validatorService.getValidatorStatus(validatorId);
        if (status != ValidatorStatus.INACTIVE) {
            throw new IllegalStateException("Withdrawal already initiated or the validator is still active");
        }

        Transaction tx = transactionRepository.findFirstByValidatorAddressOrderByTimestampDesc(validatorAddress)
                .orElseThrow(() -> new IllegalStateException("No staking transaction found for address: " + validatorAddress));

        BigInteger unstakeAmount = computeUnstakeAmount(validatorId, tx.getAmount());

        String unstakeSignature = sign(tx.getChainName().name(), validatorAddress, unstakeAmount);

        validatorService.setValidatorStatus(validatorId, ValidatorStatus.NO_STAKE);

        return new UnstakeSignatureResponse(validatorAddress, tx.getChainName().name(), unstakeAmount, unstakeSignature);
    }

    private BigInteger computeUnstakeAmount(UUID validatorId, BigInteger stakeAmount) {
        ValidatorEntity validator = validatorService.getValidator(validatorId);
        if (validator.getTotalReports() != validator.getCorrectReports()) {
            int reputationBps = validatorService.getReputationBps(validatorId);
            return BigInteger.valueOf(reputationBps)
                    .multiply(stakeAmount)
                    .divide(BigInteger.valueOf(MAX_BPS));
        }
        return stakeAmount;
    }

    private String sign(String chainName, String ethAddress, BigInteger unstakableAmount) {
        ChainConfig config = blockchainConfig.getChain(chainName);
        Credentials credentials = Credentials.create(config.getApiPrivateKey());

        // abi.encodePacked(address, uint256, uint256): 20 bytes + 32 bytes + 32 bytes = 84 bytes
        byte[] addrBytes = Numeric.hexStringToByteArray(
                ethAddress.startsWith("0x") ? ethAddress.substring(2) : ethAddress);
        byte[] amountBytes = Numeric.toBytesPadded(unstakableAmount, 32);
        byte[] packed = new byte[84];
        System.arraycopy(addrBytes, 0, packed, 0, 20);
        System.arraycopy(amountBytes, 0, packed, 20, 32);
        System.arraycopy(Numeric.toBytesPadded(
                BigInteger.valueOf(config.getChainId()), 32), 0, packed, 52, 32);

        // Hash the packed data, then sign with Ethereum message prefix
        byte[] dataHash = Hash.sha3(packed);
        Sign.SignatureData sig = Sign.signMessage(dataHash, credentials.getEcKeyPair());

        // Encode as r(32) + s(32) + v(1)
        byte[] sigBytes = new byte[65];
        System.arraycopy(sig.getR(), 0, sigBytes, 0, 32);
        System.arraycopy(sig.getS(), 0, sigBytes, 32, 32);
        sigBytes[64] = sig.getV()[0];
        return Numeric.toHexString(sigBytes);
    }
}