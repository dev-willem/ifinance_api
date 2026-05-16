package com.willembergfilho.ifinance.api.planning;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record RetirementRequest(
        @NotNull @Positive BigDecimal monthlyExpenses,
        @NotNull @PositiveOrZero BigDecimal currentSavings,
        @NotNull @PositiveOrZero BigDecimal monthlySavings,
        @NotNull @DecimalMin("0.001") @DecimalMax("1.0") BigDecimal expectedAnnualReturn,
        @NotNull @DecimalMin("0.01") @DecimalMax("0.20") BigDecimal withdrawalRate
) {}
