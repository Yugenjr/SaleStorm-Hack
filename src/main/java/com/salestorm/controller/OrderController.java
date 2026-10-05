package com.salestorm.controller;

import com.salestorm.domain.Order;
import com.salestorm.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        return ResponseEntity.ok(order);
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<Order> getOrderByPayment(@PathVariable String paymentId) {
        Order order = orderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Order not found for payment"));
        return ResponseEntity.ok(order);
    }
}
