package com.blockwin.protocol_api.validator.service;

import com.blockwin.protocol_api.common.security.AESGCMEncryptor;
import com.blockwin.protocol_api.common.utils.SecureRandomUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class APIKeyService {

    @Value("${AES_SECRET_KEY}")
    String base64SecretKey;

    public String generateAPIKey(UUID validatorId) {
        byte[] randomBytes = SecureRandomUtil.randomBytes(24);
        String randomString = Base64.getEncoder().encodeToString(randomBytes);
        String currentTimestamp = Instant.now().toString();

        String vkey = String.format("vkey_%s_%s_%s", validatorId, currentTimestamp, randomString);
        return AESGCMEncryptor.encrypt(base64SecretKey, vkey);
    }

    public UUID extractValidatorIdFromAPIKey(String apiKey) {
        String decrypted = AESGCMEncryptor.decrypt(base64SecretKey, apiKey);
        String[] data = decrypted.split("_");
        return UUID.fromString(data[0]);
    }

}
