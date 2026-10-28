package com.willembergfilho.ifinance.domain.business;

import java.math.BigDecimal;

import static com.willembergfilho.ifinance.domain.business.Ratios.*;

/** depreciation is the non-cash part of fixedCosts; desiredProfit and currentQuantity are optional (null). */
public record BreakEvenParameters(
        BigDecimal fixedCosts,
        BigDecimal unitPrice,
        BigDecimal unitVariableCost,
        BigDecimal depreciation,
        BigDecimal desiredProfit,
        BigDecimal currentQuantity
) {
    public BreakEvenParameters {
        depreciation = orZero(depreciation);
        requireNonNegative(fixedCosts, "Os custos fixos");
        requireNonNegative(unitVariableCost, "O custo variável unitário");
        requireNonNegative(depreciation, "A depreciação");
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("O preço de venda unitário deve ser maior que zero.");
        }
        if (depreciation.compareTo(fixedCosts) > 0) {
            throw new IllegalArgumentException("A depreciação não pode exceder os custos fixos.");
        }
        if (desiredProfit != null) requireNonNegative(desiredProfit, "O lucro desejado");
        if (currentQuantity != null && currentQuantity.signum() <= 0) {
            throw new IllegalArgumentException("A quantidade atual deve ser maior que zero.");
        }
    }
}
