package com.salestorm.service;

import com.salestorm.domain.Inventory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryOptimizationService implements OptimizationService {

    private final ConcurrentHashMap<String, Inventory> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> rateLimits = new ConcurrentHashMap<>();

    // We can simulate Redis failure for tests
    private volatile boolean simulateFailure = false;

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    @Override
    public boolean isAllowed(String key, int limit, long windowSeconds) {
        if (simulateFailure) return true; // fail-open
        int current = rateLimits.merge(key, 1, Integer::sum);
        return current <= limit;
    }

    @Override
    public Inventory getCachedInventory(String productId) {
        if (simulateFailure) return null;
        return cache.get(productId);
    }

    @Override
    public void cacheInventory(Inventory inventory) {
        if (simulateFailure) return;
        cache.put(inventory.getProductId(), inventory);
    }

    @Override
    public void invalidateInventory(String productId) {
        if (simulateFailure) return;
        cache.remove(productId);
    }
    
    public void reset() {
        cache.clear();
        rateLimits.clear();
        simulateFailure = false;
    }
}
