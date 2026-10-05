package com.salestorm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class HardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCorrelationIdGeneratedWhenAbsent() throws Exception {
        mockMvc.perform(get("/api/inventory/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    void testCorrelationIdPreservedWhenPresent() throws Exception {
        mockMvc.perform(get("/api/inventory/does-not-exist")
                .header("X-Correlation-ID", "test-corr-id-123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-ID", "test-corr-id-123"));
    }

    @Test
    void testActuatorHealthEndpointExposed() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testActuatorPrometheusEndpointExposed() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }
}
