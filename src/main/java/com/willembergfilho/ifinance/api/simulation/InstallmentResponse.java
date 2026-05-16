package com.willembergfilho.ifinance.api.simulation;

import com.willembergfilho.ifinance.domain.simulation.Installment;

import java.math.BigDecimal;

public record InstallmentResponse(
        int periodNumber,
        BigDecimal principalBalanceBefore,
        BigDecimal amortization,
        BigDecimal interest,
        BigDecimal additionalCharges,
        BigDecimal total,
        BigDecimal principalBalanceAfter
) {
    public static InstallmentResponse from(Installment installment) {
        return new InstallmentResponse(
                installment.periodNumber(),
                installment.principalBalanceBefore(),
                installment.amortization(),
                installment.interest(),
                installment.additionalCharges(),
                installment.total(),
                installment.principalBalanceAfter()
        );
    }
}
