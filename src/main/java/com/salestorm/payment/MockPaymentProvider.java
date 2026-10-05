package com.salestorm.payment;

import com.salestorm.domain.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockPaymentProvider implements PaymentProvider {

    // Simple mock state to test reconciliation
    private final ConcurrentHashMap<String, PaymentStatus> mockGatewayState = new ConcurrentHashMap<>();

    @Override
    public PaymentResult processPayment(String idempotencyKey, BigDecimal amount) {
        // In a real provider, we would send the idempotencyKey to the gateway.
        String transactionId = UUID.randomUUID().toString();
        
        // For testing, let's use the amount to determine the mock result
        PaymentStatus status = PaymentStatus.SUCCESS;
        if (amount.compareTo(new BigDecimal("999.00")) == 0) {
            status = PaymentStatus.FAILED;
        } else if (amount.compareTo(new BigDecimal("888.00")) == 0) {
            status = PaymentStatus.TIMEOUT;
        }

        mockGatewayState.put(transactionId, status);
        
        if (status == PaymentStatus.TIMEOUT) {
             return new PaymentResult(PaymentStatus.PENDING, transactionId);
        }

        return new PaymentResult(status, transactionId);
    }

    @Override
    public PaymentResult checkPaymentStatus(String providerTransactionId) {
        PaymentStatus status = mockGatewayState.getOrDefault(providerTransactionId, PaymentStatus.FAILED);
        return new PaymentResult(status, providerTransactionId);
    }

    // Helper method for testing reconciliation state changes
    public void setMockStatus(String transactionId, PaymentStatus status) {
        mockGatewayState.put(transactionId, status);
    }
}
