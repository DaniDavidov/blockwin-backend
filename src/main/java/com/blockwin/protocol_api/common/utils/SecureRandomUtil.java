package com.blockwin.protocol_api.common.utils;

import java.security.SecureRandom;

public class SecureRandomUtil {
    private static final SecureRandom random = new SecureRandom();

    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }
}
