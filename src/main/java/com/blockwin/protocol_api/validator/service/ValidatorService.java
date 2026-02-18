package com.blockwin.protocol_api.validator.service;

import com.blockwin.protocol_api.common.utils.ValidatorId;
import com.blockwin.protocol_api.validator.model.ValidatorChainEntity;
import com.blockwin.protocol_api.validator.model.ValidatorEntity;
import com.blockwin.protocol_api.validator.model.dto.RegisterValidatorRequest;
import com.blockwin.protocol_api.validator.model.enums.Continent;
import com.blockwin.protocol_api.validator.model.enums.Country;
import com.blockwin.protocol_api.validator.repository.ValidatorChainRepository;
import com.blockwin.protocol_api.validator.repository.ValidatorRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Service
public class ValidatorService {
    private ValidatorRepository validatorRepository;
    private ValidatorChainRepository chainRepository;

    public void registerValidator(RegisterValidatorRequest registerValidatorRequest) {

        ValidatorEntity validatorEntity = new ValidatorEntity();
        validatorEntity.setIpAddress(registerValidatorRequest.ipAddress());
        validatorEntity.setCountry(Country.valueOf(registerValidatorRequest.country()));
        validatorEntity.setContinent(Continent.valueOf(registerValidatorRequest.continent()));
        validatorEntity.setUuid(ValidatorId.generateUUID(registerValidatorRequest.chainId(), registerValidatorRequest.publicKey()));
        validatorEntity.setCreatedAt(Instant.now());
        validatorRepository.save(validatorEntity);

        ValidatorChainEntity validatorChainEntity = new ValidatorChainEntity();
        validatorChainEntity.setValidator(validatorEntity);
        validatorChainEntity.setChainId(registerValidatorRequest.chainId());
        validatorChainEntity.setChainName(registerValidatorRequest.chainName());
        validatorChainEntity.setPublicKey(registerValidatorRequest.publicKey());
        validatorChainEntity.setUuid(registerValidatorRequest.validatorChainUuid());
        chainRepository.save(validatorChainEntity);
    }
}
