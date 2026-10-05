package com.salestorm.repository;

import com.salestorm.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, String> {
    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);
}
