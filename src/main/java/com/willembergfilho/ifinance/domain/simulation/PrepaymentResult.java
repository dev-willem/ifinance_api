package com.willembergfilho.ifinance.domain.simulation;

import java.math.BigDecimal;

public record PrepaymentResult(
        int atPeriod,
        int remainingPeriods,
        BigDecimal outstandingBalance,
        BigDecimal prepaymentValue,
        BigDecimal discountAmount,
        BigDecimal discountPercent,
        BigDecimal contractedPeriodicRatePercent
) {}
