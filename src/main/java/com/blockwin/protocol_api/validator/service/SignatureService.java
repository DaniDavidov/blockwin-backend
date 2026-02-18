package com.blockwin.protocol_api.validator.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SignatureService {
    public String generateRandomMessage() {
        return UUID.randomUUID().toString();
    }
}
