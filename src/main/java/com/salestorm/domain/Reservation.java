package com.salestorm.domain;

import com.salestorm.domain.enums.ReservationStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;

@Entity
public class Reservation {
    @Id
    private String reservationId;
    
    private String productId;
    private String customerId;
    private int quantity;
    
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;
    
    private String idempotencyKey;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public Reservation() {}

    public Reservation(String reservationId, String productId, String customerId, int quantity, ReservationStatus status, String idempotencyKey, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.reservationId = reservationId;
        this.productId = productId;
        this.customerId = customerId;
        this.quantity = quantity;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
