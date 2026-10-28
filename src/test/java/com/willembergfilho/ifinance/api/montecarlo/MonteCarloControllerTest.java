package com.willembergfilho.ifinance.api.montecarlo;

import com.willembergfilho.ifinance.api.advice.GlobalExceptionHandler;
import com.willembergfilho.ifinance.application.montecarlo.MonteCarloSimulationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MonteCarloControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new MonteCarloController(new MonteCarloSimulationUseCase()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private ResultActions postJson(String body) throws Exception {
        return mvc.perform(post("/api/v1/risk/monte-carlo").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void validRequest_returnsRoundedSimulation() throws Exception {
        postJson("""
                {"initialAmount": 10000, "monthlyContribution": 500, "expectedAnnualReturn": 0.10,
                 "annualVolatility": 0.18, "years": 5, "iterations": 500, "targetAmount": 50000,
                 "annualInflation": 0.04, "seed": 42}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iterations").value(500))
                .andExpect(jsonPath("$.seed").value(42))
                .andExpect(jsonPath("$.totalContributed").value(40000.00)) // 10000 + 500·60
                .andExpect(jsonPath("$.yearlyBands.length()").value(6))
                .andExpect(jsonPath("$.histogram.length()").value(30))
                .andExpect(jsonPath("$.percentiles.p50").isNumber())
                .andExpect(jsonPath("$.nominalTarget").value(60832.65)) // 50000·1.04^5
                .andExpect(jsonPath("$.probabilityOfReachingTarget").isNumber());
    }

    @Test
    void defaultsIterations_andOmitsTargetFields() throws Exception {
        postJson("""
                {"initialAmount": 1000, "monthlyContribution": 0, "expectedAnnualReturn": 0.05,
                 "annualVolatility": 0.1, "years": 1}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iterations").value(5000))
                .andExpect(jsonPath("$.nominalTarget").doesNotExist())
                .andExpect(jsonPath("$.probabilityOfReachingTarget").doesNotExist());
    }

    @Test
    void outOfBoundsInputs_return400() throws Exception {
        postJson("""
                {"initialAmount": 1000, "monthlyContribution": 0, "expectedAnnualReturn": 0.05,
                 "annualVolatility": 2.0, "years": 61, "iterations": 50}
                """)
                .andExpect(status().isBadRequest());
        postJson("""
                {"initialAmount": -1, "monthlyContribution": 0, "expectedAnnualReturn": 0.05,
                 "annualVolatility": 0.1, "years": 10}
                """)
                .andExpect(status().isBadRequest());
        postJson("""
                {"initialAmount": 1000, "monthlyContribution": 0, "annualVolatility": 0.1, "years": 10}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void workAboveCap_returns400WithMessage() throws Exception {
        postJson("""
                {"initialAmount": 1000, "monthlyContribution": 100, "expectedAnnualReturn": 0.05,
                 "annualVolatility": 0.1, "years": 60, "iterations": 20000}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Reduza")));
    }

    @Test
    void zeroInitialAndContribution_returns400() throws Exception {
        postJson("""
                {"initialAmount": 0, "monthlyContribution": 0, "expectedAnnualReturn": 0.05,
                 "annualVolatility": 0.1, "years": 10}
                """)
                .andExpect(status().isBadRequest());
    }
}
