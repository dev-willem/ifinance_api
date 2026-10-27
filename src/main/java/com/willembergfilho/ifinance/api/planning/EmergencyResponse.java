package com.willembergfilho.ifinance.api.planning;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.planning.EmergencyReserveResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmergencyResponse(
        BigDecimal targetAmount,
        BigDecimal currentCoverageAmount,
        double currentCoverageMonths,
        double progressPercent,
        boolean targetReached,
        Integer monthsToComplete,
        BigDecimal monthlyShortfall
) {
    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static EmergencyResponse from(EmergencyReserveResult r) {
        return new EmergencyResponse(
                r.targetAmount(),
                r.currentCoverageAmount(),
                round2(r.currentCoverageMonths()),
                round2(r.progressPercent()),
                r.targetReached(),
                r.monthsToComplete(),
                r.monthlyShortfall()
        );
    }
}
