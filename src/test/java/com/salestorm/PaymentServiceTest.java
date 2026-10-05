package com.salestorm;

import com.salestorm.domain.Inventory;
import com.salestorm.domain.Payment;
import com.salestorm.domain.Reservation;
import com.salestorm.domain.enums.PaymentStatus;
import com.salestorm.domain.enums.ReservationStatus;
import com.salestorm.events.DomainEvent;
import com.salestorm.events.InMemoryEventPublisher;
import com.salestorm.payment.MockPaymentProvider;
import com.salestorm.repository.InventoryRepository;
import com.salestorm.repository.ReservationRepository;
import com.salestorm.repository.PaymentRepository;
import com.salestorm.service.InventoryService;
import com.salestorm.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InMemoryEventPublisher eventPublisher;

    @Autowired
    private MockPaymentProvider mockPaymentProvider;

    private static final String PRODUCT_ID = "prod-pay-100";
    
    private final List<DomainEvent> capturedEvents = new ArrayList<>();

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();
        
        capturedEvents.clear();
        eventPublisher.subscribe("PaymentSucceeded", capturedEvents::add);
        eventPublisher.subscribe("PaymentFailed", capturedEvents::add);

        Inventory inventory = new Inventory(PRODUCT_ID, 10, 0, 1L);
        inventoryRepository.save(inventory);
    }

    private Reservation createReservation(String customerId) {
        return inventoryService.reserve(PRODUCT_ID, customerId, 1, UUID.randomUUID().toString());
    }

    @Test
    void testSuccessfulPayment() {
        String customerId = UUID.randomUUID().toString();
        Reservation res = createReservation(customerId);
        String idempotencyKey = UUID.randomUUID().toString();
        
        // 100.00 is treated as SUCCESS by MockPaymentProvider
        Payment payment = paymentService.processPayment(res.getReservationId(), customerId, new BigDecimal("100.00"), idempotencyKey);
        
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        
        Reservation updatedRes = reservationRepository.findById(res.getReservationId()).orElseThrow();
        assertEquals(ReservationStatus.CONFIRMED, updatedRes.getStatus());
        
        assertEquals(1, capturedEvents.size());
        assertEquals("PaymentSucceeded", capturedEvents.get(0).getEventType());
    }

    @Test
    void testFailedPayment() {
        String customerId = UUID.randomUUID().toString();
        Reservation res = createReservation(customerId);
        String idempotencyKey = UUID.randomUUID().toString();
        
        // 999.00 is treated as FAILED by MockPaymentProvider
        Payment payment = paymentService.processPayment(res.getReservationId(), customerId, new BigDecimal("999.00"), idempotencyKey);
        
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        
        Reservation updatedRes = reservationRepository.findById(res.getReservationId()).orElseThrow();
        assertEquals(ReservationStatus.RELEASED, updatedRes.getStatus(), "Reservation should be released on payment failure");
        
        Inventory inv = inventoryRepository.findById(PRODUCT_ID).get();
        assertEquals(10, inv.getAvailableQuantity(), "Inventory should be restored");
        
        assertEquals(1, capturedEvents.size());
        assertEquals("PaymentFailed", capturedEvents.get(0).getEventType());
    }

    @Test
    void testPaymentTimeout() {
        String customerId = UUID.randomUUID().toString();
        Reservation res = createReservation(customerId);
        String idempotencyKey = UUID.randomUUID().toString();
        
        // 888.00 is treated as TIMEOUT by MockPaymentProvider
        Payment payment = paymentService.processPayment(res.getReservationId(), customerId, new BigDecimal("888.00"), idempotencyKey);
        
        assertEquals(PaymentStatus.PENDING, payment.getStatus(), "Payment should remain PENDING");
        
        Reservation updatedRes = reservationRepository.findById(res.getReservationId()).orElseThrow();
        assertEquals(ReservationStatus.PAYMENT_PENDING, updatedRes.getStatus());
        
        assertEquals(0, capturedEvents.size(), "No events should be published yet");
    }

    @Test
    void testDuplicatePaymentRequest() {
        String customerId = UUID.randomUUID().toString();
        Reservation res = createReservation(customerId);
        String idempotencyKey = UUID.randomUUID().toString();
        
        Payment payment1 = paymentService.processPayment(res.getReservationId(), customerId, new BigDecimal("100.00"), idempotencyKey);
        Payment payment2 = paymentService.processPayment(res.getReservationId(), customerId, new BigDecimal("100.00"), idempotencyKey);
        
        assertEquals(payment1.getPaymentId(), payment2.getPaymentId(), "Duplicate request should return the exact same payment record");
        assertEquals(1, paymentRepository.count());
    }

    @Test
    void testConcurrentDuplicatePaymentRequests() throws InterruptedException {
        String customerId = UUID.randomUUID().toString();
        Reservation res = createReservation(customerId);
        String idempotencyKey = UUID.randomUUID().toString();
        
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    paymentService.processPayment(res.getReservationId(), customerId, new BigDecimal("100.00"), idempotencyKey);
                } catch (Exception e) {
                    // Ignore exceptions for test, we just want to verify data integrity at the end
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(1, paymentRepository.count(), "Only exactly one payment record should exist for the idempotency key");
    }

    @Test
    void testSuccessfulReconciliation() {
        String customerId = UUID.randomUUID().toString();
        Reservation res = createReservation(customerId);
        String idempotencyKey = UUID.randomUUID().toString();
        
        // 1. Initial Timeout
        Payment payment = paymentService.processPayment(res.getReservationId(), customerId, new BigDecimal("888.00"), idempotencyKey);
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        
        // 2. Gateway status changes to SUCCESS out-of-band
        mockPaymentProvider.setMockStatus(payment.getProviderTransactionId(), PaymentStatus.SUCCESS);
        
        // 3. Reconcile
        Payment reconciled = paymentService.reconcilePayment(payment.getPaymentId());
        
        assertEquals(PaymentStatus.SUCCESS, reconciled.getStatus());
        Reservation updatedRes = reservationRepository.findById(res.getReservationId()).orElseThrow();
        assertEquals(ReservationStatus.CONFIRMED, updatedRes.getStatus());
        assertEquals(1, capturedEvents.size());
        assertEquals("PaymentSucceeded", capturedEvents.get(0).getEventType());
    }

    @Test
    void testFailedReconciliation() {
        String customerId = UUID.randomUUID().toString();
        Reservation res = createReservation(customerId);
        String idempotencyKey = UUID.randomUUID().toString();
        
        // 1. Initial Timeout
        Payment payment = paymentService.processPayment(res.getReservationId(), customerId, new BigDecimal("888.00"), idempotencyKey);
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        
        // 2. Gateway status changes to FAILED out-of-band
        mockPaymentProvider.setMockStatus(payment.getProviderTransactionId(), PaymentStatus.FAILED);
        
        // 3. Reconcile
        Payment reconciled = paymentService.reconcilePayment(payment.getPaymentId());
        
        assertEquals(PaymentStatus.FAILED, reconciled.getStatus());
        Reservation updatedRes = reservationRepository.findById(res.getReservationId()).orElseThrow();
        assertEquals(ReservationStatus.RELEASED, updatedRes.getStatus());
        assertEquals(1, capturedEvents.size());
        assertEquals("PaymentFailed", capturedEvents.get(0).getEventType());
        
        Inventory inv = inventoryRepository.findById(PRODUCT_ID).get();
        assertEquals(10, inv.getAvailableQuantity(), "Inventory must be restored on failed reconciliation");
    }
}
