package com.salestorm.controller;

import com.salestorm.controller.dto.ReservationRequest;
import com.salestorm.domain.Reservation;
import com.salestorm.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final InventoryService inventoryService;

    public ReservationController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<Reservation> reserve(@Valid @RequestBody ReservationRequest request) {
        Reservation reservation = inventoryService.reserve(
                request.getProductId(),
                request.getCustomerId(),
                request.getQuantity(),
                request.getIdempotencyKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @PostMapping("/{reservationId}/release")
    public ResponseEntity<Void> releaseReservation(@PathVariable String reservationId) {
        boolean released = inventoryService.releaseReservation(reservationId);
        if (released) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
