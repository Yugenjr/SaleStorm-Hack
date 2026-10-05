package com.salestorm.service;

import com.salestorm.domain.Reservation;
import com.salestorm.repository.ReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationExpiryService {

    private final ReservationRepository reservationRepository;
    private final InventoryService inventoryService;

    public ReservationExpiryService(ReservationRepository reservationRepository, InventoryService inventoryService) {
        this.reservationRepository = reservationRepository;
        this.inventoryService = inventoryService;
    }

    // Run every minute in production, explicit call in tests
    @Scheduled(fixedRate = 60000)
    public void processExpiredReservations() {
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(LocalDateTime.now());
        
        for (Reservation res : expiredReservations) {
            // Re-uses the atomic release process which handles race conditions (e.g., if a payment arrives precisely at expiry)
            inventoryService.releaseReservation(res.getReservationId());
        }
    }
}
