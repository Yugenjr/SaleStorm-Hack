package com.salestorm;

import com.salestorm.domain.Order;
import com.salestorm.domain.Payment;
import com.salestorm.domain.enums.OrderStatus;
import com.salestorm.domain.enums.PaymentStatus;
import com.salestorm.events.DomainEvent;
import com.salestorm.events.InMemoryEventPublisher;
import com.salestorm.repository.OrderRepository;
import com.salestorm.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InMemoryEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        orderService.setSimulateFailure(false);
        // Clear DLQ by processing it until empty
        while(orderService.getDlqSize() > 0) {
            orderService.setSimulateFailure(false);
            orderService.processDeadLetterQueue();
            orderRepository.deleteAll(); // Clean up if any were processed
        }
    }

    private Payment createMockPayment(PaymentStatus status) {
        return new Payment(
                UUID.randomUUID().toString(),
                "res-1",
                "cust-1",
                new BigDecimal("100.00"),
                status,
                UUID.randomUUID().toString(),
                "txn-1",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private DomainEvent createPaymentEvent(String type, Payment payment) {
        return new DomainEvent() {
            @Override
            public String getEventId() { return UUID.randomUUID().toString(); }
            @Override
            public String getEventType() { return type; }
            @Override
            public Object getPayload() { return payment; }
        };
    }

    @Test
    void testPaymentSucceededCreatesOrder() {
        Payment payment = createMockPayment(PaymentStatus.SUCCESS);
        eventPublisher.publish(createPaymentEvent("PaymentSucceeded", payment));
        
        assertEquals(1, orderRepository.count());
        Order order = orderRepository.findByPaymentId(payment.getPaymentId()).get();
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void testDuplicatePaymentSucceededEvent() {
        Payment payment = createMockPayment(PaymentStatus.SUCCESS);
        DomainEvent event = createPaymentEvent("PaymentSucceeded", payment);
        
        eventPublisher.publish(event);
        eventPublisher.publish(event); // Duplicate
        
        assertEquals(1, orderRepository.count(), "Exactly one order must be created");
    }

    @Test
    void testConcurrentDuplicatePaymentSucceededEvent() throws InterruptedException {
        Payment payment = createMockPayment(PaymentStatus.SUCCESS);
        DomainEvent event = createPaymentEvent("PaymentSucceeded", payment);
        
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    eventPublisher.publish(event);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(1, orderRepository.count(), "Only one order must be created despite concurrent duplicate events");
    }

    @Test
    void testOrderProcessingFailureLeavesEventRetryable() {
        orderService.setSimulateFailure(true);
        Payment payment = createMockPayment(PaymentStatus.SUCCESS);
        
        assertThrows(RuntimeException.class, () -> {
            orderService.handlePaymentSucceeded(createPaymentEvent("PaymentSucceeded", payment));
        });
        
        assertEquals(0, orderRepository.count());
        assertEquals(1, orderService.getDlqSize(), "Event should be in DLQ");
    }

    @Test
    void testRetryAfterFailure() {
        orderService.setSimulateFailure(true);
        Payment payment = createMockPayment(PaymentStatus.SUCCESS);
        
        assertThrows(RuntimeException.class, () -> {
            orderService.handlePaymentSucceeded(createPaymentEvent("PaymentSucceeded", payment));
        });
        assertEquals(0, orderRepository.count());
        
        // Recover service
        orderService.setSimulateFailure(false);
        orderService.processDeadLetterQueue();
        
        assertEquals(1, orderRepository.count(), "Order should be created after retry");
        assertEquals(0, orderService.getDlqSize(), "DLQ should be empty");
    }

    @Test
    void testAlreadyExistingOrder() {
        Payment payment = createMockPayment(PaymentStatus.SUCCESS);
        DomainEvent event = createPaymentEvent("PaymentSucceeded", payment);
        
        // Initial creation
        Order order1 = orderService.handlePaymentSucceeded(event);
        
        // Process again
        Order order2 = orderService.handlePaymentSucceeded(event);
        
        assertEquals(order1.getOrderId(), order2.getOrderId(), "Should return the existing order");
        assertEquals(1, orderRepository.count());
    }

    @Test
    void testPaymentFailedDoesNotCreateOrder() {
        Payment payment = createMockPayment(PaymentStatus.FAILED);
        eventPublisher.publish(createPaymentEvent("PaymentFailed", payment));
        
        assertEquals(0, orderRepository.count(), "PaymentFailed must not create an order");
    }
}
