package com.willembergfilho.ifinance.domain.planning;

import java.math.BigDecimal;

public record EmergencyReserveResult(
        BigDecimal targetAmount,
        BigDecimal currentCoverageAmount,
        double currentCoverageMonths,
        double progressPercent,
        boolean targetReached,
        Integer monthsToComplete,
        BigDecimal monthlyShortfall
) {}
