package com.willembergfilho.ifinance.domain.math;

import java.math.BigDecimal;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;

public class RateConverter {

    private static final BigDecimal ONE = BigDecimal.ONE;

    /**
     * Converts a per-period rate to an effective annual rate.
     * (1 + i_period)^periodsPerYear - 1
     */
    public BigDecimal toEffectiveAnnual(BigDecimal periodicRate, int periodsPerYear) {
        return ONE.add(periodicRate, MC)
                .pow(periodsPerYear, MC)
                .subtract(ONE, MC);
    }

    /**
     * Converts an effective annual rate to a per-period rate.
     * (1 + i_annual)^(1/periodsPerYear) - 1
     * Uses logarithms for fractional exponentiation precision.
     */
    public BigDecimal toPeriodicRate(BigDecimal effectiveAnnual, int periodsPerYear) {
        // Use double only for exponentiation; result is immediately wrapped back
        double base = ONE.add(effectiveAnnual, MC).doubleValue();
        double exp = 1.0 / periodsPerYear;
        double result = Math.pow(base, exp) - 1.0;
        return MonetaryRounding.roundRate(new BigDecimal(String.valueOf(result)));
    }

    /**
     * Converts a nominal annual rate to an effective annual rate given compounding periods.
     * (1 + i_nominal/n)^n - 1
     */
    public BigDecimal nominalToEffective(BigDecimal nominalAnnual, int compoundingPeriodsPerYear) {
        BigDecimal periodicNominal = nominalAnnual.divide(
                BigDecimal.valueOf(compoundingPeriodsPerYear), MC);
        return toEffectiveAnnual(periodicNominal, compoundingPeriodsPerYear);
    }

    /**
     * Fisher formula: real rate discounting inflation.
     * (1 + r_nominal) / (1 + inflation) - 1
     */
    public BigDecimal realRate(BigDecimal nominal, BigDecimal inflation) {
        BigDecimal num = ONE.add(nominal, MC);
        BigDecimal den = ONE.add(inflation, MC);
        return num.divide(den, MC).subtract(ONE, MC);
    }
}
