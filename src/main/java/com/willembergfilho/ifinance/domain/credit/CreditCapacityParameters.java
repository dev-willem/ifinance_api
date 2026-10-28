package com.willembergfilho.ifinance.domain.credit;

import com.willembergfilho.ifinance.domain.simulation.AmortizationSystem;

import java.math.BigDecimal;

public record CreditCapacityParameters(
        BigDecimal monthlyIncome,
        BigDecimal existingMonthlyDebts,
        BigDecimal maxCommitmentRate,
        BigDecimal annualInterestRate,
        int termMonths,
        AmortizationSystem amortizationSystem,
        BigDecimal desiredLoanAmount
) {}
