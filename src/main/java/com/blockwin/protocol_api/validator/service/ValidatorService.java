package com.blockwin.protocol_api.validator.service;

import com.blockwin.protocol_api.validator.model.ValidatorEntity;
import com.blockwin.protocol_api.validator.repository.ValidatorChainRepository;
import com.blockwin.protocol_api.validator.repository.ValidatorRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@NoArgsConstructor
@Service
public class ValidatorService {
    private ValidatorRepository validatorRepository;
    private ValidatorChainRepository chainRepository;

}
