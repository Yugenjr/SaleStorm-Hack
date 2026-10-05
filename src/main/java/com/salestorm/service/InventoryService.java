package com.salestorm.service;

import com.salestorm.domain.Reservation;
import com.salestorm.domain.enums.ReservationStatus;
import com.salestorm.repository.InventoryRepository;
import com.salestorm.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import com.salestorm.domain.Inventory;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {
    
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final OptimizationService optimizationService;
    private final MetricsService metricsService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryService.class);

    public InventoryService(InventoryRepository inventoryRepository, ReservationRepository reservationRepository, 
                            OptimizationService optimizationService, MetricsService metricsService) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.optimizationService = optimizationService;
        this.metricsService = metricsService;
    }

    public Inventory getInventory(String productId) {
        Inventory cached = optimizationService.getCachedInventory(productId);
        if (cached != null) {
            return cached;
        }
        
        Inventory dbInventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Product not found"));
        
        optimizationService.cacheInventory(dbInventory);
        return dbInventory;
    }

    @Transactional
    public Reservation reserve(String productId, String customerId, int quantity, String idempotencyKey) {
        metricsService.recordReservationAttempt();
        // 0. Rate Limiting check via OptimizationService
        if (!optimizationService.isAllowed("reserve:" + customerId, 5, 60)) {
            metricsService.recordRateLimitRejection();
            log.warn("Rate limit exceeded for customer: {}", customerId);
            throw new RateLimitExceededException("Rate limit exceeded for customer: " + customerId);
        }

        // 1. Check idempotency
        Optional<Reservation> existing = reservationRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get(); // Duplicate request, return existing reservation safely
        }

        // 2. Atomic conditional decrement in the database (Authoritative source of truth)
        int updatedRows = inventoryRepository.decrementInventoryConditionally(productId, quantity);
        
        if (updatedRows == 0) {
            metricsService.recordReservationFailure();
            log.warn("Failed to reserve product {} for customer {}: insufficient inventory", productId, customerId);
            throw new RuntimeException("Sold out or insufficient inventory");
        }
        
        // Invalidate cache since inventory has durably changed
        optimizationService.invalidateInventory(productId);

        // 3. Create Reservation
        Reservation reservation = new Reservation();
        reservation.setReservationId(UUID.randomUUID().toString());
        reservation.setProductId(productId);
        reservation.setCustomerId(customerId);
        reservation.setQuantity(quantity);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setIdempotencyKey(idempotencyKey);
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        Reservation saved = reservationRepository.save(reservation);
        metricsService.recordReservationSuccess();
        log.info("Successfully created reservation {} for product {}", saved.getReservationId(), productId);
        return saved;
    }

    @Transactional
    public boolean releaseReservation(String reservationId) {
        Optional<Reservation> optRes = reservationRepository.findById(reservationId);
        if (optRes.isEmpty()) {
            return false;
        }
        Reservation res = optRes.get();

        // Atomically attempt to change state from RESERVED to RELEASED
        int updated = reservationRepository.updateReservationStatusConditionally(
                reservationId, ReservationStatus.RESERVED, ReservationStatus.RELEASED);

        if (updated == 1) {
            // We "won" the race. It's safe to restore inventory exactly once.
            inventoryRepository.restoreInventory(res.getProductId(), res.getQuantity());
            // Invalidate cache to reflect restored inventory
            optimizationService.invalidateInventory(res.getProductId());
            return true;
        }

        // It was already released, expired, or confirmed by another process
        return false;
    }
}
