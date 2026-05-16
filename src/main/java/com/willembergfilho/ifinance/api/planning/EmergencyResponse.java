package com.willembergfilho.ifinance.api.planning;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.planning.EmergencyReserveResult;

import java.math.BigDecimal;

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
    public static EmergencyResponse from(EmergencyReserveResult r) {
        return new EmergencyResponse(
                r.targetAmount(),
                r.currentCoverageAmount(),
                r.currentCoverageMonths(),
                r.progressPercent(),
                r.targetReached(),
                r.monthsToComplete(),
                r.monthlyShortfall()
        );
    }
}
