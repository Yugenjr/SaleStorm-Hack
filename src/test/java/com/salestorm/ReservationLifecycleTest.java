package com.salestorm;

import com.salestorm.domain.Inventory;
import com.salestorm.domain.Reservation;
import com.salestorm.repository.InventoryRepository;
import com.salestorm.repository.ReservationRepository;
import com.salestorm.service.InventoryService;
import com.salestorm.service.ReservationExpiryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ReservationLifecycleTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ReservationExpiryService reservationExpiryService;

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
    void testSuccessfulRelease() {
        Reservation res = inventoryService.reserve(PRODUCT_ID, UUID.randomUUID().toString(), 1, UUID.randomUUID().toString());
        
        Inventory invAfterReserve = inventoryRepository.findById(PRODUCT_ID).get();
        assertEquals(99, invAfterReserve.getAvailableQuantity());
        assertEquals(1, invAfterReserve.getReservedQuantity());

        boolean released = inventoryService.releaseReservation(res.getReservationId());
        assertTrue(released);

        Inventory invAfterRelease = inventoryRepository.findById(PRODUCT_ID).get();
        assertEquals(100, invAfterRelease.getAvailableQuantity());
        assertEquals(0, invAfterRelease.getReservedQuantity());
    }

    @Test
    void testDuplicateRelease_OnlyRestoresInventoryOnce() {
        Reservation res = inventoryService.reserve(PRODUCT_ID, UUID.randomUUID().toString(), 1, UUID.randomUUID().toString());
        
        boolean releasedFirst = inventoryService.releaseReservation(res.getReservationId());
        assertTrue(releasedFirst);

        boolean releasedSecond = inventoryService.releaseReservation(res.getReservationId());
        assertFalse(releasedSecond, "Second release attempt should fail");

        Inventory finalInv = inventoryRepository.findById(PRODUCT_ID).get();
        assertEquals(100, finalInv.getAvailableQuantity()); // Should NOT be 101!
    }

    @Test
    void testConcurrentReleaseAttempts_OnlyRestoresOnce() throws InterruptedException {
        Reservation res = inventoryService.reserve(PRODUCT_ID, UUID.randomUUID().toString(), 1, UUID.randomUUID().toString());
        String resId = res.getReservationId();

        int numThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successReleases = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    boolean released = inventoryService.releaseReservation(resId);
                    if (released) {
                        successReleases.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        assertEquals(1, successReleases.get(), "Exactly one thread should win the race to release");
        
        Inventory finalInv = inventoryRepository.findById(PRODUCT_ID).get();
        assertEquals(100, finalInv.getAvailableQuantity());
    }

    @Test
    void testReservationExpiry() {
        Reservation res = inventoryService.reserve(PRODUCT_ID, UUID.randomUUID().toString(), 1, UUID.randomUUID().toString());
        
        // Artificially age the reservation
        res.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        reservationRepository.save(res);

        // Process expiry
        reservationExpiryService.processExpiredReservations();

        Inventory finalInv = inventoryRepository.findById(PRODUCT_ID).get();
        assertEquals(100, finalInv.getAvailableQuantity());
    }
}
