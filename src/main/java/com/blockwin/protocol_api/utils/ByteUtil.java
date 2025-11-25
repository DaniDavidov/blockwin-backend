package com.blockwin.protocol_api.utils;

import java.util.Arrays;

public class ByteUtil {

    public static byte[] combine(byte[] iv, byte[] encrypted) {
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return combined;
    }

    public static byte[] extractIV(byte[] input, int ivLength) {
        return Arrays.copyOfRange(input, 0, ivLength);
    }

    public static byte[] extractEncrypted(byte[] input, int ivLength) {
        return Arrays.copyOfRange(input, ivLength, input.length);
    }
}