package com.willembergfilho.ifinance.api.montecarlo;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record MonteCarloRequest(
        @NotNull @PositiveOrZero @DecimalMax("1000000000000") BigDecimal initialAmount,
        @NotNull @PositiveOrZero @DecimalMax("1000000000") BigDecimal monthlyContribution,
        @NotNull @DecimalMin("-0.5") @DecimalMax("1.0") BigDecimal expectedAnnualReturn,
        @NotNull @DecimalMin("0") @DecimalMax("1.5") BigDecimal annualVolatility,
        @Min(1) @Max(60) int years,
        @Min(100) @Max(20000) Integer iterations,
        @Positive @DecimalMax("1000000000000000") BigDecimal targetAmount,
        @DecimalMin("-0.1") @DecimalMax("1.0") BigDecimal annualInflation,
        @DecimalMin("-0.5") @DecimalMax("1.0") BigDecimal contributionGrowthRate,
        @PositiveOrZero @Max(9007199254740991L) Long seed
) {
    public static final int DEFAULT_ITERATIONS = 5000;
}
