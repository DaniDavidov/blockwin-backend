package com.blockwin.protocol_api.common.utils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ValidatorId {
    public static UUID generateUUID(String chainId, String publicKey) {
        String seed = ("v1:"
                + chainId
                + ":"
                + publicKey
        ).toLowerCase();

        return UUID.nameUUIDFromBytes(
                seed.getBytes(StandardCharsets.UTF_8)
        );
    }
}
