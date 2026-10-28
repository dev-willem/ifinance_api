package com.willembergfilho.ifinance.api.credit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.credit.CreditCapacityResult;
import com.willembergfilho.ifinance.domain.simulation.AmortizationSystem;

import java.math.BigDecimal;

/** Percent fields are 0..100; maxCommitmentRate and monthlyRate are decimals. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreditCapacityResponse(
        AmortizationSystem amortizationSystem,
        BigDecimal maxCommitmentRate,
        BigDecimal monthlyRate,
        BigDecimal maxInstallment,
        BigDecimal currentCommitmentPercent,
        BigDecimal maxLoanAmount,
        BigDecimal desiredLoanAmount,
        BigDecimal desiredFirstInstallment,
        BigDecimal resultingCommitmentPercent,
        Boolean approved,
        Integer minimumTermMonths,
        String statusMessage
) {
    public static CreditCapacityResponse from(CreditCapacityResult r) {
        return new CreditCapacityResponse(
                r.amortizationSystem(),
                r.maxCommitmentRate(),
                r.monthlyRate(),
                r.maxInstallment(),
                r.currentCommitmentPercent(),
                r.maxLoanAmount(),
                r.desiredLoanAmount(),
                r.desiredFirstInstallment(),
                r.resultingCommitmentPercent(),
                r.approved(),
                r.minimumTermMonths(),
                r.statusMessage()
        );
    }
}
