package com.blockwin.protocol_api.common.security;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;

@Converter
public class AESEncryptor implements AttributeConverter<String, String> {

    @Value("${AES_SECRET_KEY}")
    String base64SecretKey;

    private AESGCMEncryptor encryptor;

    // The initializer is necessary due to filed injection being executed after construction
    @PostConstruct
    public void init() {
        this.encryptor = new AESGCMEncryptor(this.base64SecretKey);
    }

    @Override
    public String convertToDatabaseColumn(String s) {
        if (s == null) {return null;}
        return encryptor.encrypt(s);
    }

    @Override
    public String convertToEntityAttribute(String s) {
        if (s == null) {return null;}
        return encryptor.decrypt(s);
    }
}
