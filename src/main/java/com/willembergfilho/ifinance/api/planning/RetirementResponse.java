package com.willembergfilho.ifinance.api.planning;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.planning.RetirementResult;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RetirementResponse(
        BigDecimal targetAmount,
        BigDecimal annualExpenses,
        BigDecimal monthlyRate,
        Integer monthsToFire,
        Integer yearsToFire,
        BigDecimal remainingMonths,
        boolean alreadyFire,
        BigDecimal projectedAmountAt10Years,
        BigDecimal projectedAmountAt20Years
) {
    public static RetirementResponse from(RetirementResult r) {
        return new RetirementResponse(
                r.targetAmount(),
                r.annualExpenses(),
                r.monthlyRate(),
                r.monthsToFire(),
                r.yearsToFire(),
                r.remainingMonths(),
                r.alreadyFire(),
                r.projectedAmountAt10Years(),
                r.projectedAmountAt20Years()
        );
    }
}
