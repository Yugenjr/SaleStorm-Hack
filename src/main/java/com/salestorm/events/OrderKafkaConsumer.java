package com.salestorm.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.salestorm.domain.Payment;
import com.salestorm.service.OrderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OrderKafkaConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrderKafkaConsumer(OrderService orderService) {
        this.orderService = orderService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "true",
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "sales.payment.events", groupId = "${spring.kafka.consumer.group-id:salestorm-order-group}")
    public void consumePaymentEvent(String message) {
        try {
            // Deserialize generically to handle map structure or specific structure
            Map<String, Object> eventMap = objectMapper.readValue(message, Map.class);
            String eventType = (String) eventMap.get("eventType");
            
            if ("PaymentSucceeded".equals(eventType)) {
                // Extract payload
                String payloadStr = objectMapper.writeValueAsString(eventMap.get("payload"));
                Payment payment = objectMapper.readValue(payloadStr, Payment.class);
                
                // Reconstruct DomainEvent for OrderService
                DomainEvent event = new DomainEvent() {
                    @Override
                    public String getEventId() { return (String) eventMap.get("eventId"); }
                    @Override
                    public String getEventType() { return eventType; }
                    @Override
                    public Object getPayload() { return payment; }
                };
                
                // Delegate to OrderService which enforces database-level idempotency
                orderService.handlePaymentSucceeded(event);
            }
            // Ignore PaymentFailed for order creation
        } catch (Exception e) {
            throw new RuntimeException("Failed to process Kafka message. Triggering retry/DLT.", e);
        }
    }
}
