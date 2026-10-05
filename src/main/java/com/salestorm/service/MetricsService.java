package com.salestorm.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private final MeterRegistry registry;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordReservationAttempt() { registry.counter("salestorm.reservations.attempts").increment(); }
    public void recordReservationSuccess() { registry.counter("salestorm.reservations.success").increment(); }
    public void recordReservationFailure() { registry.counter("salestorm.reservations.failures").increment(); }
    
    public void recordPaymentAttempt() { registry.counter("salestorm.payments.attempts").increment(); }
    public void recordPaymentSuccess() { registry.counter("salestorm.payments.success").increment(); }
    public void recordPaymentFailure() { registry.counter("salestorm.payments.failures").increment(); }
    
    public void recordOrderCreated() { registry.counter("salestorm.orders.created").increment(); }
    public void recordRateLimitRejection() { registry.counter("salestorm.ratelimit.rejections").increment(); }
}
