package com.willembergfilho.ifinance.api.credit;

import com.willembergfilho.ifinance.api.advice.GlobalExceptionHandler;
import com.willembergfilho.ifinance.application.credit.CreditCapacityUseCase;
import com.willembergfilho.ifinance.application.credit.DiscountUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreditControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new CreditController(new CreditCapacityUseCase(), new DiscountUseCase()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void capacity_ok() throws Exception {
        postJson("/api/v1/credit/capacity", """
                {"monthlyIncome":10000,"existingMonthlyDebts":1000,"annualInterestRate":0,
                 "termMonths":10,"amortizationSystem":"PRICE","desiredLoanAmount":15000}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxInstallment").value(2000.00))
                .andExpect(jsonPath("$.maxLoanAmount").value(20000.00))
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.minimumTermMonths").value(8));
    }

    @Test
    void capacity_unsupportedSystem_is400() throws Exception {
        postJson("/api/v1/credit/capacity", """
                {"monthlyIncome":10000,"existingMonthlyDebts":0,"annualInterestRate":0.1,
                 "termMonths":12,"amortizationSystem":"AMERICAN"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("PRICE ou SAC")));
    }

    @Test
    void capacity_commitmentRateOutOfBounds_is400() throws Exception {
        postJson("/api/v1/credit/capacity", """
                {"monthlyIncome":10000,"existingMonthlyDebts":0,"maxCommitmentRate":0.9,"annualInterestRate":0.1,
                 "termMonths":12,"amortizationSystem":"SAC"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("70%")));
    }

    @Test
    void capacity_termAbove480_is400() throws Exception {
        postJson("/api/v1/credit/capacity", """
                {"monthlyIncome":10000,"existingMonthlyDebts":0,"annualInterestRate":0.1,
                 "termMonths":481,"amortizationSystem":"PRICE"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void discount_ok_withInvalidCommercialSimple() throws Exception {
        postJson("/api/v1/credit/discount", """
                {"faceValue":1000,"ratePerPeriod":0.10,"periods":10}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modalities.length()").value(4))
                .andExpect(jsonPath("$.modalities[0].modality").value("RATIONAL_SIMPLE"))
                .andExpect(jsonPath("$.modalities[0].presentValue").value(500.00))
                .andExpect(jsonPath("$.modalities[1].valid").value(false))
                .andExpect(jsonPath("$.modalities[1].presentValue").doesNotExist());
    }

    @Test
    void capacity_desiredBelowOneCent_is400() throws Exception {
        postJson("/api/v1/credit/capacity", """
                {"monthlyIncome":10000,"existingMonthlyDebts":0,"annualInterestRate":0,
                 "termMonths":12,"amortizationSystem":"PRICE","desiredLoanAmount":0.001}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("R$ 0,01")));
    }

    @Test
    void discount_nearOneRateTimesTerm_is200WithInvalidModality() throws Exception {
        postJson("/api/v1/credit/discount", """
                {"faceValue":1000,"ratePerPeriod":10,"periods":0.09999999999999999999999999999999999}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modalities[1].valid").value(false))
                .andExpect(jsonPath("$.modalities[1].message", containsString("fora da faixa")));
    }

    @Test
    void discount_missingPeriods_is400() throws Exception {
        postJson("/api/v1/credit/discount", """
                {"faceValue":1000,"ratePerPeriod":0.05}""")
                .andExpect(status().isBadRequest());
    }
}
