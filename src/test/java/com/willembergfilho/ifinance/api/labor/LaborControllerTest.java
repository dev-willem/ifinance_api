package com.willembergfilho.ifinance.api.labor;

import com.willembergfilho.ifinance.api.advice.GlobalExceptionHandler;
import com.willembergfilho.ifinance.application.labor.LaborCalculationUseCase;
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

class LaborControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new LaborController(new LaborCalculationUseCase()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post("/api/v1/labor" + path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void netSalary_ok() throws Exception {
        postJson("/net-salary", "{\"grossSalary\":5000,\"dependents\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.inss.total").value(501.51))
                .andExpect(jsonPath("$.irrf.deductionMethod").value("SIMPLIFIED"))
                .andExpect(jsonPath("$.irrf.reduction").value(312.89))
                .andExpect(jsonPath("$.netSalary").value(4498.49))
                .andExpect(jsonPath("$.employerCost.employerInssRate").value(0.20))
                .andExpect(jsonPath("$.lines[0].type").value("EARNING"));
    }

    @Test
    void netSalary_missingGross_400() throws Exception {
        postJson("/net-salary", "{\"dependents\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Informe o salário bruto."));
    }

    @Test
    void netSalary_unknownYear_400() throws Exception {
        postJson("/net-salary", "{\"grossSalary\":5000,\"year\":2010}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("2010")));
    }

    @Test
    void netSalary_discountsAboveGross_422() throws Exception {
        postJson("/net-salary", "{\"grossSalary\":2000,\"otherDiscounts\":3000}")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void vacation_abonoAboveOneThird_400() throws Exception {
        postJson("/vacation", "{\"monthlySalary\":3000,\"days\":15,\"soldDays\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("1/3")));
    }

    @Test
    void vacation_ok() throws Exception {
        postJson("/vacation", "{\"monthlySalary\":6000,\"days\":20,\"soldDays\":10}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netTotal").value(7329.37));
    }

    @Test
    void thirteenth_monthsOutOfRange_400() throws Exception {
        postJson("/thirteenth", "{\"monthlySalary\":3000,\"monthsWorked\":13}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void termination_ok() throws Exception {
        postJson("/termination", """
                {"monthlySalary":3100,"admissionDate":"2023-03-10","terminationDate":"2026-06-25",
                 "terminationType":"WITHOUT_CAUSE","noticeType":"INDEMNIFIED","fgtsBalance":10000}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeDays").value(39))
                .andExpect(jsonPath("$.projectedEndDate").value("2026-08-03"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.netTotal").value(9797.28))
                .andExpect(jsonPath("$.fgts.withdrawableAmount").value(14943.22))
                .andExpect(jsonPath("$.unemploymentInsuranceEligible").value(true));
    }

    @Test
    void termination_invalidEnum_400() throws Exception {
        postJson("/termination", """
                {"monthlySalary":3100,"admissionDate":"2023-03-10","terminationDate":"2026-06-25",
                 "terminationType":"FIRED"}""")
                .andExpect(status().isBadRequest());
    }
}
