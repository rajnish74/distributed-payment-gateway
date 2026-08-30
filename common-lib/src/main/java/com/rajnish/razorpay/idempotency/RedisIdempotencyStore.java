package com.rajnish.razorpay.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
@Slf4j
@RequiredArgsConstructor
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String REDIS_KEY_PREFIX = "idempotency:";
    private final StringRedisTemplate redis;


    @Override
    public boolean setIfAbsent(String key, Duration ttl) {
        try {
            Boolean set = redis.opsForValue().setIfAbsent(REDIS_KEY_PREFIX+key, IN_PROGRESS, ttl);
            return Boolean.TRUE.equals(set);
        } catch (DataAccessException e) {
            log.warn("Idempotency store unavailable, failing open for key= {}", key, e);
            return true;
        }

    }

    @Override
    public void store(String key, String value, Duration ttl) {
        try {
            redis.opsForValue().set(REDIS_KEY_PREFIX+key, value, ttl);
        } catch (DataAccessException e) {
            log.warn("failed to persist, failing open for key= {}", key, e);
        }
    }

    @Override
    public Optional<String> get(String key) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(REDIS_KEY_PREFIX + key));
        } catch (DataAccessException e) {
            log.warn("failed to persist, failing open for key= {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void remove(String key) {
        try {
            redis.delete(REDIS_KEY_PREFIX + key);
        } catch (DataAccessException e) {
            log.warn("failed to clear idempotency key= {}", key, e);
        }
    }
}
