package com.salestorm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salestorm.controller.dto.PaymentRequest;
import com.salestorm.controller.dto.ReservationRequest;
import com.salestorm.domain.Inventory;
import com.salestorm.repository.InventoryRepository;
import com.salestorm.service.InMemoryOptimizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InMemoryOptimizationService optimizationService;

    @BeforeEach
    void setup() {
        inventoryRepository.deleteAll();
        optimizationService.reset();

        Inventory inv = new Inventory();
        inv.setProductId("api-prod-1");
        inv.setAvailableQuantity(100);
        inv.setReservedQuantity(0);
        inventoryRepository.save(inv);
    }

    @Test
    void testGetInventorySuccess() throws Exception {
        mockMvc.perform(get("/api/inventory/api-prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("api-prod-1"))
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    void testPostReservationSuccess() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setProductId("api-prod-1");
        req.setCustomerId("api-cust-1");
        req.setQuantity(2);
        req.setIdempotencyKey("api-idem-1");

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void testPostReservationInsufficientInventory() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setProductId("api-prod-1");
        req.setCustomerId("api-cust-1");
        req.setQuantity(200); // Only 100 available
        req.setIdempotencyKey("api-idem-2");

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Sold out or insufficient inventory"));
    }

    @Test
    void testDuplicateReservationIsIdempotent() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setProductId("api-prod-1");
        req.setCustomerId("api-cust-1");
        req.setQuantity(2);
        req.setIdempotencyKey("api-idem-3");

        // First request
        MvcResult res = mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        
        String resId = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.reservationId");

        // Duplicate request
        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(resId)); // Returns exact same ID
    }

    @Test
    void testReleaseReservation() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setProductId("api-prod-1");
        req.setCustomerId("api-cust-1");
        req.setQuantity(2);
        req.setIdempotencyKey("api-idem-4");

        MvcResult res = mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        
        String resId = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.reservationId");

        mockMvc.perform(post("/api/reservations/" + resId + "/release"))
                .andExpect(status().isOk());
                
        // Duplicate release yields conflict
        mockMvc.perform(post("/api/reservations/" + resId + "/release"))
                .andExpect(status().isConflict());
    }

    @Test
    void testPostPaymentAndGetStatus() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setProductId("api-prod-1");
        req.setCustomerId("api-cust-1");
        req.setQuantity(2);
        req.setIdempotencyKey("api-idem-5");

        MvcResult res = mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        
        String resId = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.reservationId");

        PaymentRequest payReq = new PaymentRequest();
        payReq.setReservationId(resId);
        payReq.setCustomerId("api-cust-1");
        payReq.setAmount(new BigDecimal("100.00"));
        payReq.setIdempotencyKey("api-pay-idem-1");

        MvcResult payRes = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS")) // Mock provider immediately succeeds for 100.00
                .andReturn();
                
        String payId = com.jayway.jsonpath.JsonPath.read(payRes.getResponse().getContentAsString(), "$.paymentId");
        
        // GET payment status
        mockMvc.perform(get("/api/payments/" + payId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testInvalidRequestYields400() throws Exception {
        ReservationRequest req = new ReservationRequest(); // missing all required fields
        
        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void testUnknownResourceYields404() throws Exception {
        mockMvc.perform(get("/api/inventory/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void testRateLimitYields429() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setProductId("api-prod-1");
        req.setCustomerId("multi-cust-1"); // Use "multi" to trigger rate limiter
        req.setQuantity(1);
        
        // Exceed the limit of 5
        for (int i=0; i<5; i++) {
            req.setIdempotencyKey("limit-idem-" + i);
            mockMvc.perform(post("/api/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        }
        
        // 6th request should fail
        req.setIdempotencyKey("limit-idem-fail");
        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"));
    }
}
