package com.salestorm.service;

import com.salestorm.domain.Inventory;
import com.salestorm.repository.InventoryRepository;
import com.salestorm.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OptimizationServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private InMemoryOptimizationService optimizationService;

    private static final String TEST_PRODUCT_ID = "opt-prod-1";
    private static final String TEST_CUSTOMER_ID = "opt-cust-1";

    @BeforeEach
    void setUp() {
        optimizationService.reset();
        inventoryRepository.deleteAll();
        reservationRepository.deleteAll();

        Inventory inv = new Inventory();
        inv.setProductId(TEST_PRODUCT_ID);
        inv.setAvailableQuantity(100);
        inv.setReservedQuantity(0);
        inventoryRepository.save(inv);
    }

    @Test
    void testCacheMissPopulatesCacheAndHitUsesCache() {
        // 1. Cache Miss -> PostgreSQL -> Redis Population
        assertNull(optimizationService.getCachedInventory(TEST_PRODUCT_ID));
        
        Inventory fromDb = inventoryService.getInventory(TEST_PRODUCT_ID);
        assertNotNull(fromDb);
        
        // Cache should now be populated
        Inventory cached = optimizationService.getCachedInventory(TEST_PRODUCT_ID);
        assertNotNull(cached);
        assertEquals(100, cached.getAvailableQuantity());
        
        // 2. Cache Hit -> PostgreSQL is not unnecessarily queried
        // We can test this implicitly: the next call returns the cached object
        Inventory hit = inventoryService.getInventory(TEST_PRODUCT_ID);
        assertEquals(cached.getAvailableQuantity(), hit.getAvailableQuantity());
    }

    @Test
    void testInventoryMutationInvalidatesCache() {
        // Pre-populate cache
        inventoryService.getInventory(TEST_PRODUCT_ID);
        assertNotNull(optimizationService.getCachedInventory(TEST_PRODUCT_ID));

        // 3 & 4. Reservation mutates DB (authoritative) and invalidates cache
        inventoryService.reserve(TEST_PRODUCT_ID, TEST_CUSTOMER_ID, 5, "idem-1");
        
        assertNull(optimizationService.getCachedInventory(TEST_PRODUCT_ID));
        
        Inventory updatedDb = inventoryService.getInventory(TEST_PRODUCT_ID);
        assertEquals(95, updatedDb.getAvailableQuantity());
    }

    @Test
    void testRateLimiterLimitsRequests() {
        // 7. Rate limiter allows requests under limit
        for (int i = 0; i < 5; i++) {
            inventoryService.reserve(TEST_PRODUCT_ID, TEST_CUSTOMER_ID + "-multi", 1, "idem-multi-" + i);
        }

        // 8. Rate limiter rejects requests over the limit
        assertThrows(RateLimitExceededException.class, () -> {
            inventoryService.reserve(TEST_PRODUCT_ID, TEST_CUSTOMER_ID + "-multi", 1, "idem-multi-blocked");
        });
    }

    @Test
    void testRedisFailureDoesNotCorruptInventory() {
        // Pre-populate DB
        inventoryService.getInventory(TEST_PRODUCT_ID);
        
        // Simulate Redis failure
        optimizationService.setSimulateFailure(true);
        
        // Cache should return null (miss), rate limit should allow (fail open)
        Inventory inv = inventoryService.getInventory(TEST_PRODUCT_ID);
        assertNotNull(inv); // Fetched from DB safely
        
        // 10. Redis failure does not cause inventory overselling, reservation works
        assertDoesNotThrow(() -> {
            inventoryService.reserve(TEST_PRODUCT_ID, "cust-fail-open", 10, "idem-fail-open");
        });
        
        Inventory updated = inventoryService.getInventory(TEST_PRODUCT_ID);
        assertEquals(90, updated.getAvailableQuantity());
    }
}
