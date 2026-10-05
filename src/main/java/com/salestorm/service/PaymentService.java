package com.salestorm.service;

import com.salestorm.domain.Payment;
import com.salestorm.domain.Reservation;
import com.salestorm.domain.enums.PaymentStatus;
import com.salestorm.domain.enums.ReservationStatus;
import com.salestorm.events.DomainEvent;
import com.salestorm.events.EventPublisher;
import com.salestorm.payment.PaymentProvider;
import com.salestorm.payment.PaymentResult;
import com.salestorm.repository.PaymentRepository;
import com.salestorm.repository.ReservationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final InventoryService inventoryService;
    private final PaymentProvider paymentProvider;
    private final EventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository, 
                          ReservationRepository reservationRepository,
                          InventoryService inventoryService,
                          PaymentProvider paymentProvider, 
                          EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.inventoryService = inventoryService;
        this.paymentProvider = paymentProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Payment processPayment(String reservationId, String customerId, BigDecimal amount, String idempotencyKey) {
        // 1. Idempotency Check (Read)
        Optional<Payment> existingPaymentOpt = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPaymentOpt.isPresent()) {
            return existingPaymentOpt.get(); // Duplicate request
        }

        // 2. Validate Reservation and change state to PAYMENT_PENDING
        int updated = reservationRepository.updateReservationStatusConditionally(reservationId, ReservationStatus.RESERVED, ReservationStatus.PAYMENT_PENDING);
        if (updated == 0) {
            throw new RuntimeException("Reservation is not in a valid state for payment.");
        }

        // 3. Create initial pending payment record
        Payment payment = new Payment(
                UUID.randomUUID().toString(),
                reservationId,
                customerId,
                amount,
                PaymentStatus.PENDING,
                idempotencyKey,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        try {
            payment = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            // Concurrent duplicate idempotency key insertion
            return paymentRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        }

        // 4. Call Payment Provider
        PaymentResult result = paymentProvider.processPayment(idempotencyKey, amount);
        
        payment.setStatus(result.getStatus());
        payment.setProviderTransactionId(result.getTransactionId());
        payment.setUpdatedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        // 5. Handle Result
        handlePaymentResult(payment);

        return payment;
    }

    @Transactional
    public Payment reconcilePayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment;
        }

        PaymentResult result = paymentProvider.checkPaymentStatus(payment.getProviderTransactionId());
        
        if (result.getStatus() == PaymentStatus.SUCCESS || result.getStatus() == PaymentStatus.FAILED) {
            payment.setStatus(result.getStatus());
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);
            handlePaymentResult(payment);
        }
        
        return payment;
    }

    private void handlePaymentResult(Payment payment) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            // Confirm reservation
            int updated = reservationRepository.updateReservationStatusConditionally(payment.getReservationId(), ReservationStatus.PAYMENT_PENDING, ReservationStatus.CONFIRMED);
            if (updated == 1) {
                publishEvent("PaymentSucceeded", payment);
            }
        } else if (payment.getStatus() == PaymentStatus.FAILED) {
            // Change reservation back to RESERVED so releaseReservation can release it atomically
            int reverted = reservationRepository.updateReservationStatusConditionally(payment.getReservationId(), ReservationStatus.PAYMENT_PENDING, ReservationStatus.RESERVED);
            if (reverted == 1) {
                inventoryService.releaseReservation(payment.getReservationId());
                publishEvent("PaymentFailed", payment);
            }
        }
        // If PENDING/TIMEOUT, we leave it alone for reconciliation
    }

    private void publishEvent(String eventType, Payment payment) {
        eventPublisher.publish(new DomainEvent() {
            @Override
            public String getEventId() { return UUID.randomUUID().toString(); }
            
            @Override
            public String getEventType() { return eventType; }
            
            @Override
            public Object getPayload() { return payment; }
        });
    }
}
