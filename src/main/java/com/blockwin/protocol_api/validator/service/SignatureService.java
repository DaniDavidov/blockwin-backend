package com.blockwin.protocol_api.validator.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SignatureService {
    private final ConcurrentHashMap<String, String> activeChallenges = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupWorker = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    void initializeCleanupWorker() {
        cleanupWorker.scheduleAtFixedRate(
                this::removeExpiredChallenges,
                1, 1, TimeUnit.MINUTES
        );
    }

    public String generateRandomMessage() {
        String challengeId = UUID.randomUUID().toString();
        long expiry = Instant.now().plusSeconds(60).toEpochMilli();
        String message = String.format("challenge_%s_%d", challengeId, expiry);
        activeChallenges.put(challengeId, message);
        return message;
    }

    public boolean verifySignature(
            String message,
            String signature,
            String expectedAddress
    ) {
        if (!isChallengeValid(message)) {
            return false;
        }

        try {
            // Recover the signing address from signature
            String recoveredAddress = recoverAddress(message, signature);

            // Compare with expected address
            return expectedAddress.equalsIgnoreCase(recoveredAddress);

        } catch (Exception e) {
            return false;
        }
    }

    private String recoverAddress(String message, String signature)
            throws SignatureException {

        String prefixedMessage = "\u0019Ethereum Signed Message:\n" +
                message.length() +
                message;
        byte[] messageHash = Hash.sha3(prefixedMessage.getBytes(StandardCharsets.UTF_8));

        // Parse signature (v, r, s components)
        byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
        byte v = signatureBytes[64];
        if (v < 27) {
            v += 27;  // Normalize v value
        }

        Sign.SignatureData signatureData = new Sign.SignatureData(
                v,
                Arrays.copyOfRange(signatureBytes, 0, 32),   // r
                Arrays.copyOfRange(signatureBytes, 32, 64)   // s
        );

        // Recover public key
        BigInteger publicKey = Sign.signedMessageHashToKey(messageHash, signatureData);

        // Derive address from public key
        return "0x" + Keys.getAddress(publicKey);
    }

    private boolean isChallengeValid(String message) {
        String[] data = message.split("_");
        String challengeId = data[1];
        String value = activeChallenges.get(challengeId);
        return value != null;
    }

    private void removeExpiredChallenges() {
        activeChallenges.entrySet().removeIf(entry -> {
            String s = entry.getValue().split("_")[2];
            long expiry = Long.parseLong(s);
            return Instant.now().toEpochMilli() > expiry;
        });
    }
}
