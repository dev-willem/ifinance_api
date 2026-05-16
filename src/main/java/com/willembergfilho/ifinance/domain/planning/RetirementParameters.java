package com.willembergfilho.ifinance.domain.planning;

import java.math.BigDecimal;

public record RetirementParameters(
        BigDecimal monthlyExpenses,
        BigDecimal currentSavings,
        BigDecimal monthlySavings,
        BigDecimal expectedAnnualReturn,
        BigDecimal withdrawalRate
) {}
