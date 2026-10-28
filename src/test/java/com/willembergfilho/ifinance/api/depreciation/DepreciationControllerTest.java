package com.willembergfilho.ifinance.api.depreciation;

import com.willembergfilho.ifinance.api.advice.GlobalExceptionHandler;
import com.willembergfilho.ifinance.application.depreciation.DepreciationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Duration;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepreciationControllerTest {

    private static final String BASE = "/api/v1/business/depreciation";

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new DepreciationController(new DepreciationUseCase()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void calculate_straightLine_returnsSchedule() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"assetValue": 10000, "residualValue": 1000, "usefulLifeYears": 5,
                         "method": "STRAIGHT_LINE"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("STRAIGHT_LINE"))
                .andExpect(jsonPath("$.annualRatePercent").value(20.0))
                .andExpect(jsonPath("$.rows", hasSize(5)))
                .andExpect(jsonPath("$.rows[0].depreciation").value(1800.0))
                .andExpect(jsonPath("$.rows[4].closingBookValue").value(1000.0))
                .andExpect(jsonPath("$.totalDepreciation").value(9000.0));
    }

    @Test
    void calculate_missingMethodAndShiftsOutOfRange_returns400() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"assetValue": 10000, "residualValue": 1000, "usefulLifeYears": 5, "shiftsPerDay": 4}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("método")))
                .andExpect(jsonPath("$.message", containsString("turnos")));
    }

    @Test
    void calculate_residualNotBelowAsset_returns400FromDomain() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"assetValue": 1000, "residualValue": 1000, "usefulLifeYears": 5,
                         "method": "STRAIGHT_LINE"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O valor residual deve ser menor que o valor do ativo."));
    }

    @Test
    void calculate_hugeExponentsAndSubCentValues_returns400Quickly() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                            {"assetValue": 100, "residualValue": 1E+30000000, "usefulLifeYears": 5,
                             "method": "STRAIGHT_LINE"}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("residual")));
            mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                            {"assetValue": 10000, "residualValue": 0, "usefulLifeYears": 5,
                             "method": "UNITS_OF_PRODUCTION", "totalUnits": 1E+30000000,
                             "unitsPerYear": [0.5, 1E+30000000]}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("unidades")));
            mvc.perform(post(BASE + "/compare").contentType(MediaType.APPLICATION_JSON).content("""
                            {"assetValue": 1000, "residualValue": 1E-30000000, "usefulLifeYears": 5}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("casas decimais")));
        });
    }

    @Test
    void compare_returnsThreeMethods() throws Exception {
        mvc.perform(post(BASE + "/compare").contentType(MediaType.APPLICATION_JSON).content("""
                        {"assetValue": 10000, "residualValue": 0, "usefulLifeYears": 5}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].method").value("STRAIGHT_LINE"))
                .andExpect(jsonPath("$[1].method").value("SUM_OF_YEARS_DIGITS"))
                .andExpect(jsonPath("$[2].method").value("DECLINING_BALANCE"))
                .andExpect(jsonPath("$[2].rows[3].depreciation").value(1080.0));
    }

    @Test
    void assetClasses_listsReceitaTable() throws Exception {
        mvc.perform(get(BASE + "/asset-classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BUILDINGS"))
                .andExpect(jsonPath("$[0].annualRatePercent").value(4))
                .andExpect(jsonPath("$[0].usefulLifeYears").value(25));
    }
}
