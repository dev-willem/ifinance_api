package com.willembergfilho.ifinance.domain.planning;

import java.math.BigDecimal;

public record RetirementResult(
        BigDecimal targetAmount,
        BigDecimal annualExpenses,
        BigDecimal monthlyRate,
        Integer monthsToFire,
        Integer yearsToFire,
        BigDecimal remainingMonths,
        boolean alreadyFire,
        BigDecimal projectedAmountAt10Years,
        BigDecimal projectedAmountAt20Years
) {}
