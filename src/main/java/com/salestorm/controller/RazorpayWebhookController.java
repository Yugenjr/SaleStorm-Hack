package com.salestorm.controller;

import com.razorpay.Utils;
import com.salestorm.domain.Payment;
import com.salestorm.domain.enums.PaymentStatus;
import com.salestorm.repository.PaymentRepository;
import com.salestorm.service.PaymentService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/payments/webhook")
public class RazorpayWebhookController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final String webhookSecret;

    public RazorpayWebhookController(PaymentService paymentService, 
                                     PaymentRepository paymentRepository,
                                     @Value("${razorpay.webhook.secret:testsecret}") String webhookSecret) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        
        try {
            if (signature == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing signature");
            }

            // 1. Verify Signature
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            JSONObject json = new JSONObject(payload);
            String event = json.getString("event");
            
            JSONObject paymentEntity = json.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");
            
            String orderId = paymentEntity.getString("order_id");

            // 2. Find payment by providerTransactionId
            Optional<Payment> optPayment = paymentRepository.findByProviderTransactionId(orderId);
            if (optPayment.isEmpty()) {
                return ResponseEntity.ok("Ignored");
            }
            
            Payment payment = optPayment.get();
            
            // 3. Webhook idempotency and State machine rules
            if (payment.getStatus() != PaymentStatus.PENDING) {
                return ResponseEntity.ok("Already processed");
            }

            // 4. Map event to status and trigger reconciliation/update via atomic PaymentService method
            if ("payment.captured".equals(event)) {
                paymentService.reconcilePaymentWithWebhook(payment.getPaymentId(), PaymentStatus.SUCCESS);
            } else if ("payment.failed".equals(event)) {
                paymentService.reconcilePaymentWithWebhook(payment.getPaymentId(), PaymentStatus.FAILED);
            }
            
            return ResponseEntity.ok("Processed");
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }
}
