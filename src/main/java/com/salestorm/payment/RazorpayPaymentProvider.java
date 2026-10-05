package com.salestorm.payment;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.salestorm.domain.enums.PaymentStatus;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "razorpay")
public class RazorpayPaymentProvider implements PaymentProvider {

    private final RazorpayClient client;

    public RazorpayPaymentProvider(
            @Value("${razorpay.key.id:}") String keyId,
            @Value("${razorpay.key.secret:}") String keySecret) throws RazorpayException {
        // Only initialize if keys are provided to prevent crash on mock profile startup
        if (keyId != null && !keyId.isEmpty()) {
            this.client = new RazorpayClient(keyId, keySecret);
        } else {
            this.client = null;
        }
    }

    public RazorpayPaymentProvider(RazorpayClient client) {
        this.client = client;
    }

    @Override
    public PaymentResult processPayment(String idempotencyKey, BigDecimal amount) {
        try {
            JSONObject orderRequest = new JSONObject();
            // Razorpay uses paise, multiply by 100
            orderRequest.put("amount", amount.multiply(new BigDecimal("100")).intValue()); 
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", idempotencyKey);
            
            Order order = client.orders.create(orderRequest);
            return new PaymentResult(PaymentStatus.PENDING, order.get("id"));
        } catch (Exception e) {
            return new PaymentResult(PaymentStatus.FAILED, null);
        }
    }

    @Override
    public PaymentResult checkPaymentStatus(String providerTransactionId) {
        try {
            Order order = client.orders.fetch(providerTransactionId);
            String status = order.get("status");
            
            if ("paid".equalsIgnoreCase(status)) {
                return new PaymentResult(PaymentStatus.SUCCESS, providerTransactionId);
            } else if ("created".equalsIgnoreCase(status) || "attempted".equalsIgnoreCase(status)) {
                return new PaymentResult(PaymentStatus.PENDING, providerTransactionId);
            } else {
                return new PaymentResult(PaymentStatus.FAILED, providerTransactionId);
            }
        } catch (Exception e) {
            // Unresolvable state, keep pending for later reconciliation
            return new PaymentResult(PaymentStatus.PENDING, providerTransactionId);
        }
    }
}
