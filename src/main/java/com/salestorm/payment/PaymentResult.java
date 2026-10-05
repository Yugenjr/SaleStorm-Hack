package com.salestorm.payment;

import com.salestorm.domain.enums.PaymentStatus;

public class PaymentResult {
    private PaymentStatus status;
    private String transactionId;

    public PaymentResult(PaymentStatus status, String transactionId) {
        this.status = status;
        this.transactionId = transactionId;
    }

    public PaymentStatus getStatus() { return status; }
    public String getTransactionId() { return transactionId; }
}
