package com.salestorm.domain;

import com.salestorm.domain.enums.ReservationStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
