package com.willembergfilho.ifinance.domain.simulation;

import java.math.BigDecimal;

public record CetCharge(
        String description,
        ChargeType chargeType,
        BigDecimal amount,
        Integer appliesOnPeriod   // null = applies to all periods
) {
    public enum ChargeType {
        FIXED,
        PERCENTAGE
    }
}
