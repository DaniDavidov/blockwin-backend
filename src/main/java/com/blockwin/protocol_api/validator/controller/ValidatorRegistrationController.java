package com.blockwin.protocol_api.validator.controller;

import com.blockwin.protocol_api.validator.model.dto.RegisterValidatorRequest;
import com.blockwin.protocol_api.validator.model.dto.RegisterValidatorResponse;
import com.blockwin.protocol_api.validator.service.SignatureService;
import com.blockwin.protocol_api.validator.service.ValidatorService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/validator")
@CrossOrigin("*")
public class ValidatorRegistrationController {
    private final ValidatorService validatorService;
    private final SignatureService signatureService;

    @GetMapping("/challenge")
    public ResponseEntity<String> getChallenge() {
        return ResponseEntity.ok(signatureService.generateRandomMessage());
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterValidatorResponse> registerValidator(RegisterValidatorRequest registerValidatorRequest) {
        // TODO implement validation and registration mechanism
        return ResponseEntity.ok(null);
    }
}
