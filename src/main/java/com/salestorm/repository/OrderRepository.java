package com.salestorm.repository;

import com.salestorm.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    Optional<Order> findByPaymentId(String paymentId);
}
