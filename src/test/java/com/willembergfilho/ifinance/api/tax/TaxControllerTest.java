package com.willembergfilho.ifinance.api.tax;

import com.willembergfilho.ifinance.api.advice.GlobalExceptionHandler;
import com.willembergfilho.ifinance.application.tax.SimplesNacionalUseCase;
import com.willembergfilho.ifinance.application.tax.TaxRegimeCompareUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaxControllerTest {

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new TaxController(new SimplesNacionalUseCase(), new TaxRegimeCompareUseCase()))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    private ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post("/api/v1/tax" + path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void simples_returnsDasAndRepartition() throws Exception {
        postJson("/simples", """
                {"rbt12": 300000, "monthlyRevenue": 25000, "annex": "III"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.bracket").value(2))
                .andExpect(jsonPath("$.effectiveRate").value(0.0808))
                .andExpect(jsonPath("$.dasAmount").value(2020.0))
                .andExpect(jsonPath("$.shares[?(@.tax == 'ISS')].amount").value(646.4))
                .andExpect(jsonPath("$.disclaimer").value(containsString("Reforma Tributária")));
    }

    @Test
    void simples_aboveLimit_omitsDasFields() throws Exception {
        postJson("/simples", """
                {"rbt12": 5000000, "monthlyRevenue": 400000, "annex": "I"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.dasAmount").doesNotExist());
    }

    @Test
    void simples_invalidBody_returns400() throws Exception {
        postJson("/simples", """
                {"rbt12": -1, "monthlyRevenue": 1000, "annex": "I"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("RBT12")));

        postJson("/simples", """
                {"rbt12": 1000, "monthlyRevenue": 1000, "annex": "VI"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void compare_returnsCheapestRegime() throws Exception {
        postJson("/compare", """
                {"annualRevenue": 600000, "activity": "COMMERCE", "annualPayroll": 60000,
                 "deductibleExpenses": 300000, "salesTaxRate": 0.12}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cheapest").value("SIMPLES_NACIONAL"))
                .andExpect(jsonPath("$.regimes.length()").value(3))
                .andExpect(jsonPath("$.regimes[0].total").value(43140.0));
    }

    @Test
    void compare_domainViolation_returns400() throws Exception {
        postJson("/compare", """
                {"annualRevenue": 600000, "activity": "COMMERCE", "deductibleExpenses": 1000, "creditablePurchases": 2000}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("PIS/Cofins")));
    }

    @Test
    void exponentLiterals_areRejectedBeforeRounding() throws Exception {
        // Short literals that would expand to millions of digits in setScale(2)
        postJson("/simples", """
                {"rbt12": 100000, "monthlyRevenue": 1e1000000, "annex": "I"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Receita do mês")));

        postJson("/compare", """
                {"annualRevenue": 1e1000000, "activity": "COMMERCE"}
                """)
                .andExpect(status().isBadRequest());

        postJson("/compare", """
                {"annualRevenue": 600000, "activity": "COMMERCE", "salesTaxRate": 1e-1000000}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("ICMS/ISS")));
    }

    @Test
    void compare_acceptsBinaryFloatNoiseInRates() throws Exception {
        // 1.1 / 100 in JavaScript
        postJson("/compare", """
                {"annualRevenue": 600000, "activity": "COMMERCE", "salesTaxRate": 0.011000000000000001}
                """)
                .andExpect(status().isOk());
    }

    @Test
    void compare_rateAbove100Percent_returns400() throws Exception {
        postJson("/compare", """
                {"annualRevenue": 600000, "activity": "SERVICES", "salesTaxRate": 5}
                """)
                .andExpect(status().isBadRequest());
    }
}
