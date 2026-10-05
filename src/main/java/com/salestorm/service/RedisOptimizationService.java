package com.salestorm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salestorm.domain.Inventory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisOptimizationService implements OptimizationService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String CACHE_PREFIX = "inv:cache:";
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    public RedisOptimizationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean isAllowed(String key, int limit, long windowSeconds) {
        try {
            String redisKey = RATE_LIMIT_PREFIX + key;
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }
            return count != null && count <= limit;
        } catch (Exception e) {
            // Fail open: if Redis is down, we don't want to block legitimate requests.
            // PostgreSQL will still protect inventory via atomic updates.
            return true;
        }
    }

    @Override
    public Inventory getCachedInventory(String productId) {
        try {
            String data = redisTemplate.opsForValue().get(CACHE_PREFIX + productId);
            if (data != null) {
                return objectMapper.readValue(data, Inventory.class);
            }
        } catch (Exception e) {
            // Log error and fail open (cache miss)
        }
        return null;
    }

    @Override
    public void cacheInventory(Inventory inventory) {
        try {
            String data = objectMapper.writeValueAsString(inventory);
            // Simple 60 second TTL for prototype
            redisTemplate.opsForValue().set(CACHE_PREFIX + inventory.getProductId(), data, 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Ignore cache write failure
        }
    }

    @Override
    public void invalidateInventory(String productId) {
        try {
            redisTemplate.delete(CACHE_PREFIX + productId);
        } catch (Exception e) {
            // Ignore cache invalidate failure
        }
    }
}
