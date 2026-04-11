package com.blockwin.protocol_api.validator.service;

import com.blockwin.protocol_api.blockchain.service.TransactionManagementService;
import com.blockwin.protocol_api.common.utils.ValidatorId;
import com.blockwin.protocol_api.validator.model.ValidatorChainEntity;
import com.blockwin.protocol_api.validator.model.ValidatorEntity;
import com.blockwin.protocol_api.validator.model.dto.RegisterValidatorRequest;
import com.blockwin.protocol_api.validator.model.enums.ChainName;
import com.blockwin.protocol_api.validator.model.enums.Continent;
import com.blockwin.protocol_api.validator.model.enums.Country;
import com.blockwin.protocol_api.validator.model.enums.ValidatorStatus;
import com.blockwin.protocol_api.validator.repository.ValidatorChainRepository;
import com.blockwin.protocol_api.validator.repository.ValidatorRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.blockwin.protocol_api.common.utils.Constants.*;

@AllArgsConstructor
@Service
public class ValidatorService {
    private final TransactionManagementService transactionManagementService;
    private ValidatorRepository validatorRepository;
    private ValidatorChainRepository chainRepository;

    @Transactional
    public UUID registerValidator(RegisterValidatorRequest registerValidatorRequest) {
        transactionManagementService.validateDeposit(registerValidatorRequest);

        ValidatorEntity validatorEntity = new ValidatorEntity();
        validatorEntity.setIpAddress(registerValidatorRequest.ipAddress());
        validatorEntity.setCountry(Country.valueOf(registerValidatorRequest.country()));
        validatorEntity.setContinent(Continent.valueOf(registerValidatorRequest.continent()));
        validatorEntity.setUuid(ValidatorId.generateUUID(registerValidatorRequest.chainId(), registerValidatorRequest.publicKey()));
        validatorEntity.setStatus(ValidatorStatus.INACTIVE);
        validatorEntity.setCreatedAt(Instant.now());
        ValidatorEntity savedValidator = validatorRepository.saveAndFlush(validatorEntity);

        ValidatorChainEntity validatorChainEntity = new ValidatorChainEntity();
        validatorChainEntity.setValidator(validatorEntity);
        validatorChainEntity.setChainId(registerValidatorRequest.chainId());
        validatorChainEntity.setChainName(ChainName.valueOf(registerValidatorRequest.chainName()));
        validatorChainEntity.setPublicKey(registerValidatorRequest.publicKey());
        chainRepository.saveAndFlush(validatorChainEntity);

        return savedValidator.getUuid();
    }

    public ValidatorEntity getValidator(UUID validatorId) {
        return validatorRepository
                .findById(validatorId)
                .orElseThrow(() -> new RuntimeException("Validator not found"));
    }

    public void setValidatorStatus(UUID validatorId, ValidatorStatus validatorStatus) {
        ValidatorEntity validator = getValidator(validatorId);
        validator.setStatus(validatorStatus);
        validatorRepository.save(validator);
    }

    public ValidatorStatus getValidatorStatus(UUID validatorId) {
        return getValidator(validatorId).getStatus();
    }

    /**
     * Returns the validator's current reputation in basis points (0–10 000).
     * Returns 0 if the validator has never submitted a report.
     */
    public int getReputationBps(UUID validatorId) {
        ValidatorEntity validator = getValidator(validatorId);
        if (validator.getTotalReports() == 0) return 0;
        long correctReports = validator.getCorrectReports() + VIRTUAL_CORRECT_REPORTS;
        long totalReports = validator.getTotalReports() + VIRTUAL_TOTAL_REPORTS;
        return (int) (correctReports * MAX_BPS / totalReports);
    }

    /**
     * Returns the validator's Ethereum address for a given chain, or empty if not registered.
     * Used by the reward module to resolve on-chain identities without exposing repositories.
     */
    public Optional<String> getEthAddress(UUID validatorId, ChainName chainName) {
        return chainRepository.findByValidator_Uuid(validatorId)
                .stream()
                .filter(e -> chainName.equals(e.getChainName()))
                .findFirst()
                .map(ValidatorChainEntity::getPublicKey);
    }
}
