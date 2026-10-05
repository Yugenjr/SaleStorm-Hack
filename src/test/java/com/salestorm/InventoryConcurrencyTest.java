package com.salestorm;

import com.salestorm.domain.Inventory;
import com.salestorm.repository.InventoryRepository;
import com.salestorm.repository.ReservationRepository;
import com.salestorm.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class InventoryConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private ReservationRepository reservationRepository;

    private static final String PRODUCT_ID = "prod-100";

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();
        
        Inventory inventory = new Inventory(PRODUCT_ID, 100, 0, 1L);
        inventoryRepository.save(inventory);
    }

    @Test
    void testConcurrentReservations_NoOverselling() throws InterruptedException {
        int totalRequests = 10000;
        ExecutorService executor = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        String previousIdempKey = UUID.randomUUID().toString();

        System.out.println("Starting Concurrency Simulation (Java/Spring Boot)...");
        System.out.println("Initial inventory: 100");
        System.out.println("Concurrent requests: " + totalRequests);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final String customerId = "cust-" + i;
            
            // Introduce some duplicates (5%)
            final String idempotencyKey;
            if (i % 20 == 0 && i > 0) {
                idempotencyKey = previousIdempKey;
            } else {
                idempotencyKey = UUID.randomUUID().toString();
                previousIdempKey = idempotencyKey;
            }

            executor.submit(() -> {
                try {
                    inventoryService.reserve(PRODUCT_ID, customerId, 1, idempotencyKey);
                    successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();

        Inventory finalInventory = inventoryRepository.findById(PRODUCT_ID).orElseThrow();
        long actualReservations = reservationRepository.count();

        System.out.println("--------------------------------------------------");
        System.out.println("Total requests processed: " + (successCount.get() + failureCount.get()));
        System.out.println("Successful reservations (including idemp dups): " + successCount.get());
        System.out.println("Failed reservations: " + failureCount.get());
        System.out.println("Final available inventory: " + finalInventory.getAvailableQuantity());
        System.out.println("Final reserved quantity: " + finalInventory.getReservedQuantity());
        System.out.println("Total distinct reservations in DB: " + actualReservations);
        System.out.println("Oversold: " + Math.max(0, actualReservations - 100));
        System.out.println("Time taken: " + (endTime - startTime) + " ms");
        System.out.println("--------------------------------------------------");

        assertEquals(0, finalInventory.getAvailableQuantity(), "Available inventory should be exactly 0");
        assertEquals(100, finalInventory.getReservedQuantity(), "Reserved inventory should be exactly 100");
        assertEquals(100, actualReservations, "There should be exactly 100 successful unique reservations");
    }
}
