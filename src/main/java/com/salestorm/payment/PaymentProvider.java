package com.salestorm.payment;

import java.math.BigDecimal;

public interface PaymentProvider {
    PaymentResult processPayment(String idempotencyKey, BigDecimal amount);
    PaymentResult checkPaymentStatus(String providerTransactionId);
}
