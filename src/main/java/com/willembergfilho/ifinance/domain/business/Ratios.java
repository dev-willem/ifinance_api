package com.willembergfilho.ifinance.domain.business;

import com.willembergfilho.ifinance.domain.math.MonetaryRounding;

import java.math.BigDecimal;

public final class Ratios {

    public static final int RATIO_SCALE = 6;

    private Ratios() {}

    /** numerator / denominator at 6 decimal places, or null when the denominator is zero. */
    public static BigDecimal of(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) return null;
        return numerator.divide(denominator, MonetaryRounding.MC).setScale(RATIO_SCALE, MonetaryRounding.MODE);
    }

    static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    static void requireNonNegative(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(label + " deve ser maior ou igual a zero.");
        }
    }

    static void requireRate(BigDecimal value, String label) {
        if (value == null || value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(label + " deve estar entre 0% e 100%.");
        }
    }
}
