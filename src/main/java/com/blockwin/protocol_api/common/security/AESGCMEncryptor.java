package com.blockwin.protocol_api.common.security;

import com.blockwin.protocol_api.common.utils.ByteUtil;
import com.blockwin.protocol_api.common.utils.SecureRandomUtil;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESGCMEncryptor {
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes - recommended for GCM

    public static String encrypt(String base64SecretKey, String plaintext) {
        SecretKey secretKey = getSecretKey(base64SecretKey);
        try {
            byte[] iv = SecureRandomUtil.randomBytes(IV_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes());

            byte[] combined = ByteUtil.combine(iv, encrypted);
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
        }
    }

    public static String decrypt(String base64SecretKey, String ciphertext) {
        SecretKey secretKey = getSecretKey(base64SecretKey);
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            byte[] iv = ByteUtil.extractIV(decoded, IV_LENGTH);
            byte[] encrypted = ByteUtil.extractEncrypted(decoded, IV_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            return new String(cipher.doFinal(encrypted));

        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decryption failed", e);
        }
    }

    private static SecretKey getSecretKey(String base64SecretKey) {
        byte[] keyBytes = Base64.getDecoder().decode(base64SecretKey);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
    }
}
