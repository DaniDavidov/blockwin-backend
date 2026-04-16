package com.blockwin.protocol_api.validator.controller;

import com.blockwin.protocol_api.validator.model.dto.*;
import com.blockwin.protocol_api.blockchain.service.SignatureService;
import com.blockwin.protocol_api.validator.service.ValidatorService;
import com.blockwin.protocol_api.validator.service.ValidatorStakingService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/validator")
@CrossOrigin("*")
public class ValidatorRegistrationController {
    private final ValidatorService validatorService;
    private final SignatureService signatureService;
    private final ValidatorStakingService validatorStakingService;

    @GetMapping("/challenge")
    public ResponseEntity<String> getChallenge() {
        return ResponseEntity.ok(signatureService.generateRandomMessage());
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterValidatorResponse> registerValidator(@RequestBody RegisterValidatorRequest registerValidatorRequest) {
        UUID validatorId = validatorService.registerValidator(registerValidatorRequest);
        return ResponseEntity.ok(new RegisterValidatorResponse(validatorId));
    }

    @PostMapping("/stake/verify")
    public ResponseEntity<StakeVerificationResponse> verifyStake(@RequestBody StakeVerificationRequest request) {
        return ResponseEntity.ok(validatorStakingService.verifyStake(request));
    }

    @PostMapping("/unstake-signature")
    public ResponseEntity<UnstakeSignatureResponse> getUnstakeSignature(@RequestBody UnstakeRequest request) {
        return ResponseEntity.ok(validatorStakingService.generateUnstakeSignature(request.message(), request.signature()));
    }
}