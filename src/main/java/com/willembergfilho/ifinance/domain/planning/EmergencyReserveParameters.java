package com.willembergfilho.ifinance.domain.planning;

import java.math.BigDecimal;

public record EmergencyReserveParameters(
        BigDecimal monthlyExpenses,
        int monthsCoverage,
        BigDecimal currentSavings,
        BigDecimal monthlySavings,
        BigDecimal expectedAnnualReturn
) {}
