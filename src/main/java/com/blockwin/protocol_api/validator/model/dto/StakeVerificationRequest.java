package com.blockwin.protocol_api.validator.model.dto;


public record StakeVerificationRequest(String txHash, String message, String signature) {}