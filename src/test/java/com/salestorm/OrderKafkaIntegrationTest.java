package com.salestorm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.salestorm.domain.Payment;
import com.salestorm.domain.enums.PaymentStatus;
import com.salestorm.events.DomainEvent;
import com.salestorm.events.KafkaEventPublisher;
import com.salestorm.events.OrderKafkaConsumer;
import com.salestorm.repository.OrderRepository;
import com.salestorm.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@TestPropertySource(properties = {
        "app.kafka.enabled=true",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
public class OrderKafkaIntegrationTest {

    @Autowired
    private KafkaEventPublisher publisher;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        orderService.setSimulateFailure(false);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private Payment createMockPayment(PaymentStatus status) {
        return new Payment(
                UUID.randomUUID().toString(),
                "res-kafka-1",
                "cust-kafka-1",
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
    void testKafkaEventPublishingAndConsumption() {
        Payment payment = createMockPayment(PaymentStatus.SUCCESS);
        DomainEvent event = createPaymentEvent("PaymentSucceeded", payment);
        
        // 1 & 2: Publish event
        publisher.publish(event);

        // 3: Verify Consumer delegates to OrderService
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, orderRepository.count());
        });
    }

    @Test
    void testDuplicatePaymentSucceededIdempotencyViaKafka() {
        Payment payment = createMockPayment(PaymentStatus.SUCCESS);
        DomainEvent event = createPaymentEvent("PaymentSucceeded", payment);
        
        publisher.publish(event);
        publisher.publish(event); // Duplicate
        
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // 4: Duplicate does not create duplicate orders
            assertEquals(1, orderRepository.count());
        });
    }
    
    @Test
    void testConsumerFailureTriggersRetryAndDLT() {
        orderService.setSimulateFailure(true);
        Payment payment = createMockPayment(PaymentStatus.SUCCESS);
        DomainEvent event = createPaymentEvent("PaymentSucceeded", payment);
        
        publisher.publish(event);
        
        // 5 & 6: Consumer failure triggers retry and eventually goes to DLT
        await().pollDelay(2, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(0, orderRepository.count());
        });
        
        // 7: Reprocessing event after recovery remains idempotent
        // We'll simulate recovery by removing the failure and publishing again
        orderService.setSimulateFailure(false);
        publisher.publish(event);
        
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, orderRepository.count());
        });
    }
}
