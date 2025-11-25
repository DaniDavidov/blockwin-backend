package com.blockwin.protocol_api.service;

import com.blockwin.protocol_api.utils.ByteUtil;
import com.blockwin.protocol_api.utils.SecureRandomUtil;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESGCMEncryptor {
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes - recommended for GCM

    private final SecretKey secretKey;

    public AESGCMEncryptor(String base64SecretKey) {
        byte[] keyBytes = Base64.getDecoder().decode(base64SecretKey);
        this.secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
    }


    public String encrypt(String plaintext) {
        try {
            byte[] iv = SecureRandomUtil.randomBytes(IV_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, this.secretKey, spec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes());

            byte[] combined = ByteUtil.combine(iv, encrypted);
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            byte[] iv = ByteUtil.extractIV(decoded, IV_LENGTH);
            byte[] encrypted = ByteUtil.extractEncrypted(decoded, IV_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, this.secretKey, spec);

            return new String(cipher.doFinal(encrypted));

        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decryption failed", e);
        }
    }
}
