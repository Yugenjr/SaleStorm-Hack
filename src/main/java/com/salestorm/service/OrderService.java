package com.salestorm.service;

import com.salestorm.domain.Order;
import com.salestorm.domain.Payment;
import com.salestorm.domain.enums.OrderStatus;
import com.salestorm.events.DomainEvent;
import com.salestorm.events.InMemoryEventPublisher;
import com.salestorm.repository.OrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InMemoryEventPublisher eventPublisher;

    // Simple in-memory queue to demonstrate retry/recovery without a real Message Broker
    private final ConcurrentLinkedQueue<DomainEvent> deadLetterQueue = new ConcurrentLinkedQueue<>();
    
    // Simulate service failure for testing recovery
    private volatile boolean simulateFailure = false;

    public OrderService(OrderRepository orderRepository, InMemoryEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void init() {
        eventPublisher.subscribe("PaymentSucceeded", this::handlePaymentSucceeded);
    }

    @Transactional
    public Order handlePaymentSucceeded(DomainEvent event) {
        if (simulateFailure) {
            deadLetterQueue.add(event);
            throw new RuntimeException("Simulated OrderService failure. Event added to DLQ.");
        }

        Payment payment = (Payment) event.getPayload();
        
        // 1. Idempotency Check (Read)
        Optional<Order> existingOrderOpt = orderRepository.findByPaymentId(payment.getPaymentId());
        if (existingOrderOpt.isPresent()) {
            return existingOrderOpt.get(); // Duplicate event, ignore
        }

        // 2. Create Order
        Order order = new Order(
                UUID.randomUUID().toString(),
                payment.getCustomerId(),
                payment.getPaymentId(),
                payment.getReservationId(),
                OrderStatus.CREATED,
                LocalDateTime.now()
        );
        
        try {
            order = orderRepository.saveAndFlush(order);
            return order;
        } catch (DataIntegrityViolationException e) {
            // 3. Idempotency Check (Write) - handle concurrent duplicate event insertion
            return orderRepository.findByPaymentId(payment.getPaymentId()).orElseThrow();
        }
    }

    public void processDeadLetterQueue() {
        int size = deadLetterQueue.size();
        for (int i = 0; i < size; i++) {
            DomainEvent event = deadLetterQueue.poll();
            if (event != null) {
                try {
                    handlePaymentSucceeded(event);
                } catch (Exception e) {
                    deadLetterQueue.add(event); // Re-queue if still failing
                }
            }
        }
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
    
    public int getDlqSize() {
        return deadLetterQueue.size();
    }
}
