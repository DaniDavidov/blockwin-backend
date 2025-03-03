package com.blockwin.protocol_api.model.dto;

public record RegisterRequest(String email, String password, String firstName, String lastName, String phoneNumber) {}
