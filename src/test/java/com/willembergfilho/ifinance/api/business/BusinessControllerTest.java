package com.willembergfilho.ifinance.api.business;

import com.willembergfilho.ifinance.api.advice.GlobalExceptionHandler;
import com.willembergfilho.ifinance.application.business.BreakEvenUseCase;
import com.willembergfilho.ifinance.application.business.IncomeStatementUseCase;
import com.willembergfilho.ifinance.application.business.MarkupUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BusinessControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(
                        new BusinessController(new IncomeStatementUseCase(), new BreakEvenUseCase(), new MarkupUseCase()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post("/api/v1/business" + path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void incomeStatement_ok() throws Exception {
        postJson("/income-statement", """
                {"grossRevenue":100000,"salesDeductionsRate":0.10,"costOfGoodsSold":40000,
                 "sellingExpenses":5000,"administrativeExpenses":15000,"otherOperatingExpenses":2000,
                 "depreciationAmortization":3000,"financialRevenue":1000,"financialExpenses":6000,
                 "months":2,"monthlyRevenueGrowth":0.05}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netIncome").value(13200.0))
                .andExpect(jsonPath("$.projection.length()").value(2));
    }

    @Test
    void incomeStatement_invalidMonths_is400() throws Exception {
        postJson("/income-statement", """
                {"grossRevenue":1000,"costOfGoodsSold":100,"months":61}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A projeção pode ter no máximo 60 meses."));
    }

    @Test
    void breakEven_priceBelowVariableCost_is422() throws Exception {
        postJson("/break-even", """
                {"fixedCosts":1000,"unitPrice":10,"unitVariableCost":12}""")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("custo variável unitário")));
    }

    @Test
    @Timeout(5)
    void extremeExponents_are400_beforeAnyArithmetic() throws Exception {
        // Without @Digits these tied up the thread for minutes (huge BigInteger in setScale/round).
        postJson("/break-even", """
                {"fixedCosts":1e99999999,"unitPrice":10,"unitVariableCost":5}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("custos fixos excedem o limite")));
        postJson("/income-statement", """
                {"grossRevenue":1000,"costOfGoodsSold":100,"salesDeductionsRate":1e-99999999}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("deduções aceita no máximo 10 casas")));
        postJson("/markup", """
                {"unitCost":60,"salesTaxRate":0.14300000000000002}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void breakEven_missingPrice_is400() throws Exception {
        postJson("/break-even", """
                {"fixedCosts":1000,"unitVariableCost":5}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Informe o preço de venda unitário."));
    }

    @Test
    void markup_ok_omitsNullFields() throws Exception {
        postJson("/markup", """
                {"unitCost":60,"salesTaxRate":0.10,"desiredProfitRate":0.30}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedPrice").value(100.0))
                .andExpect(jsonPath("$.composition[0].label").value("Custo unitário"))
                .andExpect(jsonPath("$.actualProfitMargin").doesNotExist());
    }

    @Test
    void markup_ratesAt100Percent_is422() throws Exception {
        postJson("/markup", """
                {"unitCost":60,"salesTaxRate":0.40,"desiredProfitRate":0.60}""")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("inferior a 100%")));
    }

    @Test
    void markup_blankExtraRateName_is400() throws Exception {
        postJson("/markup", """
                {"unitCost":60,"extraRates":[{"name":"","rate":0.1}]}""")
                .andExpect(status().isBadRequest());
    }
}
