package com.willembergfilho.ifinance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SimulationControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    WebApplicationContext wac;

    @Autowired
    ObjectMapper objectMapper;

    MockMvc mvc;

    private static final String BASE = "/api/v1/simulations";

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // ---------------------------------------------------------------- POST --

    @Test
    void createSimulation_sac_returnsCreated() throws Exception {
        String body = """
                {
                  "name": "SAC Integration Test",
                  "amortizationSystem": "SAC",
                  "principal": 100000,
                  "interestRate": 1.0,
                  "rateType": "EFFECTIVE",
                  "term": 12,
                  "periodicity": "MONTHLY",
                  "cetEnabled": false,
                  "inflationCorrectionEnabled": false,
                  "charges": []
                }
                """;

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.installments").isArray())
                .andExpect(jsonPath("$.installments.length()").value(12))
                .andExpect(jsonPath("$.totalPaid").isNumber());
    }

    @Test
    void createSimulation_price_returnsCreated() throws Exception {
        String body = """
                {
                  "name": "PRICE Integration Test",
                  "amortizationSystem": "PRICE",
                  "principal": 50000,
                  "interestRate": 12.0,
                  "rateType": "NOMINAL",
                  "term": 24,
                  "periodicity": "MONTHLY",
                  "cetEnabled": false,
                  "inflationCorrectionEnabled": false,
                  "charges": []
                }
                """;

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.installments.length()").value(24));
    }

    @Test
    void createSimulation_missingName_returnsBadRequest() throws Exception {
        String body = """
                {
                  "amortizationSystem": "SAC",
                  "principal": 100000,
                  "interestRate": 1.0,
                  "rateType": "EFFECTIVE",
                  "term": 12,
                  "periodicity": "MONTHLY",
                  "cetEnabled": false,
                  "inflationCorrectionEnabled": false,
                  "charges": []
                }
                """;

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    void createSimulation_missingPrincipal_returnsBadRequest() throws Exception {
        String body = """
                {
                  "name": "Sem principal",
                  "amortizationSystem": "SAC",
                  "interestRate": 1.0,
                  "rateType": "EFFECTIVE",
                  "term": 12,
                  "periodicity": "MONTHLY",
                  "cetEnabled": false,
                  "inflationCorrectionEnabled": false,
                  "charges": []
                }
                """;

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------------- GET --

    @Test
    void getById_afterCreate_returnsSimulation() throws Exception {
        String body = """
                {
                  "name": "GET Test",
                  "amortizationSystem": "SAC",
                  "principal": 10000,
                  "interestRate": 1.0,
                  "rateType": "EFFECTIVE",
                  "term": 6,
                  "periodicity": "MONTHLY",
                  "cetEnabled": false,
                  "inflationCorrectionEnabled": false,
                  "charges": []
                }
                """;

        String result = mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(result).get("id").asText();

        mvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.installments.length()").value(6));
    }

    @Test
    void getById_unknownId_returnsNotFound() throws Exception {
        mvc.perform(get(BASE + "/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void history_returnsPagedList() throws Exception {
        mvc.perform(get(BASE + "/history"))
                .andExpect(status().isOk());
    }
}
