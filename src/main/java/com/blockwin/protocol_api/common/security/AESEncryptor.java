package com.blockwin.protocol_api.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;

@Converter
public class AESEncryptor implements AttributeConverter<String, String> {

    @Value("${AES_SECRET_KEY}")
    String base64SecretKey;

    @Override
    public String convertToDatabaseColumn(String s) {
        if (s == null) {return null;}
        return AESGCMEncryptor.encrypt(base64SecretKey, s);
    }

    @Override
    public String convertToEntityAttribute(String s) {
        if (s == null) {return null;}
        return AESGCMEncryptor.decrypt(base64SecretKey, s);
    }
}
