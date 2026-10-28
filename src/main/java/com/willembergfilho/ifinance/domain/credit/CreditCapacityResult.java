package com.willembergfilho.ifinance.domain.credit;

import com.willembergfilho.ifinance.domain.simulation.AmortizationSystem;

import java.math.BigDecimal;

/** Percent fields are expressed as 0..100 with 2 decimals; monthlyRate is a decimal. */
public record CreditCapacityResult(
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
) {}
