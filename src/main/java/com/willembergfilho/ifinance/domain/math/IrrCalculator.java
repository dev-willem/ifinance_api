package com.willembergfilho.ifinance.domain.math;

import java.math.BigDecimal;
import java.util.List;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;

public class IrrCalculator {

    private static final int MAX_ITERATIONS = 1000;
    private static final BigDecimal CONVERGENCE = new BigDecimal("0.0000000001");
    private static final BigDecimal DERIVATIVE_TOLERANCE = new BigDecimal("0.0000001");
    private static final BigDecimal INITIAL_RATE = new BigDecimal("0.1");

    private final NpvCalculator npvCalculator = new NpvCalculator();

    /**
     * Calculates the Internal Rate of Return (IRR) for the given cash flows.
     * Uses Newton-Raphson with bisection fallback.
     * cashFlows[0] must be negative (initial outflow).
     */
    public BigDecimal calculate(List<BigDecimal> cashFlows) {
        try {
            return newtonRaphson(cashFlows);
        } catch (CalculationException e) {
            return bisection(cashFlows);
        }
    }

    private BigDecimal newtonRaphson(List<BigDecimal> cashFlows) {
        BigDecimal rate = INITIAL_RATE;

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            BigDecimal npv = npvCalculator.calculate(cashFlows, rate);
            BigDecimal dnpv = npvCalculator.derivative(cashFlows, rate);

            if (dnpv.abs(MC).compareTo(DERIVATIVE_TOLERANCE) < 0) {
                throw new CalculationException("Derivative too small, switching to bisection.");
            }

            BigDecimal next = rate.subtract(npv.divide(dnpv, MC), MC);

            if (next.subtract(rate).abs(MC).compareTo(CONVERGENCE) < 0) {
                return MonetaryRounding.roundRate(next);
            }
            rate = next;
        }
        throw new CalculationException("Newton-Raphson did not converge.");
    }

    private BigDecimal bisection(List<BigDecimal> cashFlows) {
        BigDecimal low = new BigDecimal("-0.9999");
        BigDecimal high = new BigDecimal("10.0");

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            BigDecimal mid = low.add(high, MC).divide(new BigDecimal("2"), MC);
            BigDecimal npv = npvCalculator.calculate(cashFlows, mid);

            if (npv.abs(MC).compareTo(CONVERGENCE) < 0) {
                return MonetaryRounding.roundRate(mid);
            }

            BigDecimal npvLow = npvCalculator.calculate(cashFlows, low);
            if (npv.signum() == npvLow.signum()) {
                low = mid;
            } else {
                high = mid;
            }

            if (high.subtract(low, MC).abs(MC).compareTo(CONVERGENCE) < 0) {
                return MonetaryRounding.roundRate(mid);
            }
        }
        throw new CalculationException("IRR did not converge for the given cash flows.");
    }
}
