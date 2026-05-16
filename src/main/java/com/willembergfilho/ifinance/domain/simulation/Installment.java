package com.willembergfilho.ifinance.domain.simulation;

import java.math.BigDecimal;

public record Installment(
        int periodNumber,
        BigDecimal principalBalanceBefore,
        BigDecimal amortization,
        BigDecimal interest,
        BigDecimal additionalCharges,
        BigDecimal total,
        BigDecimal principalBalanceAfter
) {
    public Installment withAdditionalCharges(BigDecimal charges) {
        BigDecimal newTotal = total.add(charges);
        return new Installment(periodNumber, principalBalanceBefore, amortization,
                interest, charges, newTotal, principalBalanceAfter);
    }

    public Installment withCorrectedTotal(BigDecimal correctedTotal) {
        return new Installment(periodNumber, principalBalanceBefore, amortization,
                interest, additionalCharges, correctedTotal, principalBalanceAfter);
    }
}
