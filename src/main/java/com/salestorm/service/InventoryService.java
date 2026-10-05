package com.salestorm.service;

import com.salestorm.domain.Reservation;
import com.salestorm.domain.enums.ReservationStatus;
import com.salestorm.repository.InventoryRepository;
import com.salestorm.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {
    
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;

    public InventoryService(InventoryRepository inventoryRepository, ReservationRepository reservationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public Reservation reserve(String productId, String customerId, int quantity, String idempotencyKey) {
        // 1. Check idempotency
        Optional<Reservation> existing = reservationRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get(); // Duplicate request, return existing reservation safely
        }

        // 2. Atomic conditional decrement in the database
        int updatedRows = inventoryRepository.decrementInventoryConditionally(productId, quantity);
        
        if (updatedRows == 0) {
            throw new RuntimeException("Sold out or insufficient inventory");
        }

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

        return reservationRepository.save(reservation);
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
            return true;
        }

        // It was already released, expired, or confirmed by another process
        return false;
    }
}
