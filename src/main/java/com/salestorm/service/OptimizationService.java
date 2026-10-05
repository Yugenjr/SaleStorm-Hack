package com.salestorm.service;

import com.salestorm.domain.Inventory;

public interface OptimizationService {
    boolean isAllowed(String key, int limit, long windowSeconds);
    Inventory getCachedInventory(String productId);
    void cacheInventory(Inventory inventory);
    void invalidateInventory(String productId);
}
