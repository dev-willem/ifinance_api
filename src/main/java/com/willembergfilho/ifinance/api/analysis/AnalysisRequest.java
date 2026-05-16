package com.willembergfilho.ifinance.api.analysis;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record AnalysisRequest(
        @NotEmpty(message = "cashFlows must not be empty")
        @Size(min = 2, message = "cashFlows must have at least 2 entries")
        List<BigDecimal> cashFlows,

        BigDecimal discountRate
) {}
