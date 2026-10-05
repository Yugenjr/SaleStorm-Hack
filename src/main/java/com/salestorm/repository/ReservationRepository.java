package com.salestorm.repository;

import com.salestorm.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.salestorm.domain.enums.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, String> {
    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reservation r SET r.status = :newStatus WHERE r.reservationId = :reservationId AND r.status = :oldStatus")
    int updateReservationStatusConditionally(@Param("reservationId") String reservationId, 
                                             @Param("oldStatus") ReservationStatus oldStatus, 
                                             @Param("newStatus") ReservationStatus newStatus);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'RESERVED' AND r.expiresAt < :now")
    List<Reservation> findExpiredReservations(@Param("now") LocalDateTime now);
}
