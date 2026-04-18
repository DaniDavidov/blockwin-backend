package com.blockwin.protocol_api.consensus.cache;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@AllArgsConstructor
@Service
public class ValidatorReputationCacheService {

    private final StringRedisTemplate redis;

    private String key(UUID validatorId) {
        return "validator:rep:" + validatorId;
    }

    public void cacheValidator(UUID validatorId,
                               int reliabilityBps) {

        Map<String, String> map = Map.of(
                "reliabilityBps", String.valueOf(reliabilityBps)
        );

        redis.opsForHash().putAll(key(validatorId), map);
    }

    public void cacheValidators(Map<UUID, Integer> reputations) {
        if (reputations.isEmpty()) {
            return;
        }
        byte[] field = "reliabilityBps".getBytes();
        redis.executePipelined((RedisCallback<?>) connection -> {
            for (Map.Entry<UUID, Integer> e : reputations.entrySet()) {
                byte[] keyBytes = key(e.getKey()).getBytes();
                byte[] value = String.valueOf(e.getValue()).getBytes();
                connection.hashCommands().hSet(keyBytes, field, value);
            }
            return null;
        });
    }

    public Map<UUID, Integer> fetchReputations(Set<UUID> validatorIds) {

        List<Object> results = redis.executePipelined((RedisCallback<?>) connection -> {
            for (UUID id : validatorIds) {
                String key = "validator:rep:" + id;
                connection.hashCommands().hGet(
                        key.getBytes(),
                        "reliabilityBps".getBytes()
                );
            }
            return null;
        });

        Iterator<UUID> iterator = validatorIds.iterator();
        Map<UUID, Integer> reputations = new HashMap<>();

        for (Object r : results) {
            UUID id = iterator.next();
            reputations.put(
                    id,
                    r == null ? 5000 : Integer.parseInt(new String((byte[]) r))
            );
        }
        return reputations;
    }
}