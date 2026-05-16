package com.willembergfilho.ifinance.api.simulation;

import com.willembergfilho.ifinance.domain.simulation.PrepaymentResult;

import java.math.BigDecimal;

public record PrepaymentResponse(
        int atPeriod,
        int remainingPeriods,
        BigDecimal outstandingBalance,
        BigDecimal prepaymentValue,
        BigDecimal discountAmount,
        BigDecimal discountPercent,
        BigDecimal contractedPeriodicRatePercent
) {
    public static PrepaymentResponse from(PrepaymentResult result) {
        return new PrepaymentResponse(
                result.atPeriod(),
                result.remainingPeriods(),
                result.outstandingBalance(),
                result.prepaymentValue(),
                result.discountAmount(),
                result.discountPercent(),
                result.contractedPeriodicRatePercent()
        );
    }
}
