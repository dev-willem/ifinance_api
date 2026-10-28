package com.willembergfilho.ifinance.api.risk;

import com.willembergfilho.ifinance.api.advice.GlobalExceptionHandler;
import com.willembergfilho.ifinance.application.risk.RiskMetricsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RiskMetricsControllerTest {

    private static final String URL = "/api/v1/risk/metrics";
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new RiskMetricsController(new RiskMetricsUseCase()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String body) throws Exception {
        return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void returnsMetricsWithBenchmark() throws Exception {
        postJson("""
                {"values":[0.10,-0.05,0.02,-0.03],"inputType":"RETURNS",
                 "benchmarkValues":[0.10,-0.05,0.02,-0.03],"periodicity":"ANNUAL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periods").value(4))
                .andExpect(jsonPath("$.meanPeriodReturn").value(0.01))
                .andExpect(jsonPath("$.confidenceLevel").value(0.95))
                .andExpect(jsonPath("$.drawdownRecoveryIndex").value(nullValue()))
                .andExpect(jsonPath("$.benchmark.beta").value(1.0))
                .andExpect(jsonPath("$.benchmark.informationRatio").value(nullValue()))
                .andExpect(jsonPath("$.wealthIndex", hasSize(5)));
    }

    @Test
    void zeroVariance_serializesNullsNotNaN() throws Exception {
        postJson("""
                {"values":[0.01,0.01,0.01],"inputType":"RETURNS","periodicity":"DAILY"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharpeRatio").value(nullValue()))
                .andExpect(jsonPath("$.calmarRatio").value(nullValue()))
                .andExpect(jsonPath("$.benchmark").value(nullValue()));
    }

    @Test
    void tooFewValues_returns400() throws Exception {
        postJson("""
                {"values":[0.01],"inputType":"RETURNS","periodicity":"DAILY"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("entre 2 e 5000")));
    }

    @Test
    void missingPeriodicity_returns400() throws Exception {
        postJson("""
                {"values":[0.01,0.02],"inputType":"RETURNS"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("periodicidade")));
    }

    @Test
    void twoPrices_returns422() throws Exception {
        postJson("""
                {"values":[100,101],"inputType":"PRICES","periodicity":"DAILY"}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message", containsString("ao menos 2 retornos")));
    }

    @Test
    void misalignedBenchmark_returns400() throws Exception {
        postJson("""
                {"values":[0.01,0.02,0.03],"benchmarkValues":[0.01,0.02],"inputType":"RETURNS","periodicity":"DAILY"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("mesmo número")));
    }
}
