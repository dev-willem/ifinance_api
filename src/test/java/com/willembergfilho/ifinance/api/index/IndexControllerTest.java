package com.willembergfilho.ifinance.api.index;

import com.willembergfilho.ifinance.api.advice.GlobalExceptionHandler;
import com.willembergfilho.ifinance.application.index.FetchIndexRateUseCase;
import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.index.IndexRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IndexControllerTest {

    private FetchIndexRateUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(FetchIndexRateUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new IndexController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void currentAnnualizesMonthlyIndex() throws Exception {
        // IPCA 0.50% a.m. -> (1.005^12 - 1) * 100 = 6.1678 -> 6.17% a.a.
        when(useCase.fetchCurrent(EconomicIndex.IPCA)).thenReturn(Optional.of(
                new IndexRate(EconomicIndex.IPCA, LocalDate.of(2026, 9, 1), new BigDecimal("0.50"), false)));

        mvc.perform(get("/api/v1/indexes/IPCA/current"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.index").value("IPCA"))
                .andExpect(jsonPath("$.referenceDate").value("2026-09-01"))
                .andExpect(jsonPath("$.annualRate").value(6.17))
                .andExpect(jsonPath("$.isProjection").value(false));
    }

    @Test
    void currentAnnualizesDailyIndexOver252BusinessDays() throws Exception {
        // CDI 0.055131% a.d. -> (1.00055131^252 - 1) * 100 = 14.8999... -> 14.90% a.a.
        when(useCase.fetchCurrent(EconomicIndex.CDI)).thenReturn(Optional.of(
                new IndexRate(EconomicIndex.CDI, LocalDate.of(2026, 10, 27), new BigDecimal("0.055131"), false)));

        mvc.perform(get("/api/v1/indexes/CDI/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annualRate").value(14.90));
    }

    @Test
    void currentWithoutDataIs404() throws Exception {
        when(useCase.fetchCurrent(EconomicIndex.TR)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/indexes/TR/current")).andExpect(status().isNotFound());
    }

    @Test
    void unknownIndexIs400ListingAcceptedValues() throws Exception {
        mvc.perform(get("/api/v1/indexes/FOO/current"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("IPCA, IGP_M, CDI, SELIC, TR")));
        verifyNoInteractions(useCase);
    }

    @Test
    void rangeReturnsAnnualizedList() throws Exception {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 2, 1);
        when(useCase.fetchRange(EconomicIndex.IPCA, from, to)).thenReturn(List.of(
                new IndexRate(EconomicIndex.IPCA, from, new BigDecimal("0.50"), false),
                new IndexRate(EconomicIndex.IPCA, to, new BigDecimal("0.50"), true)));

        mvc.perform(get("/api/v1/indexes/IPCA").param("from", "2026-01-01").param("to", "2026-02-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].referenceDate").value("2026-02-01"))
                .andExpect(jsonPath("$[1].isProjection").value(true));
    }

    @Test
    void rangeWithFromAfterToIs400() throws Exception {
        mvc.perform(get("/api/v1/indexes/IPCA").param("from", "2026-03-01").param("to", "2026-02-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Intervalo inválido")));
        verify(useCase, never()).fetchRange(any(), any(), any());
    }

    @Test
    void rangeWithoutDatesIs400() throws Exception {
        mvc.perform(get("/api/v1/indexes/IPCA").param("from", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("'to'")));
    }

    @Test
    void rangeWithMalformedDateIs400() throws Exception {
        mvc.perform(get("/api/v1/indexes/IPCA").param("from", "01/01/2026").param("to", "2026-02-01"))
                .andExpect(status().isBadRequest());
    }
}
