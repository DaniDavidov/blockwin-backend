package com.blockwin.protocol_api.validator.controller;

import com.blockwin.protocol_api.validator.model.dto.RegisterValidatorRequest;
import com.blockwin.protocol_api.validator.model.dto.RegisterValidatorResponse;
import com.blockwin.protocol_api.blockchain.service.SignatureService;
import com.blockwin.protocol_api.validator.service.APIKeyService;
import com.blockwin.protocol_api.validator.service.ValidatorService;
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
    private final APIKeyService apiKeyService;

    @GetMapping("/challenge")
    public ResponseEntity<String> getChallenge() {
        return ResponseEntity.ok(signatureService.generateRandomMessage());
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterValidatorResponse> registerValidator(@RequestBody RegisterValidatorRequest registerValidatorRequest) {
        UUID uuid = validatorService.registerValidator(registerValidatorRequest);
        String apiKey = apiKeyService.generateAPIKey(uuid);
        return ResponseEntity.ok(new RegisterValidatorResponse(apiKey));
    }
}
