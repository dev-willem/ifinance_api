package com.willembergfilho.ifinance.api.planning;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record EmergencyRequest(
        @NotNull @Positive BigDecimal monthlyExpenses,
        @Min(3) @Max(24) int monthsCoverage,
        @NotNull @PositiveOrZero BigDecimal currentSavings,
        @NotNull @PositiveOrZero BigDecimal monthlySavings,
        @DecimalMin("0") @DecimalMax("1.0") BigDecimal expectedAnnualReturn
) {}
