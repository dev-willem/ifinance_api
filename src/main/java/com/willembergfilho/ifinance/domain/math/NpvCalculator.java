package com.willembergfilho.ifinance.domain.math;

import java.math.BigDecimal;
import java.util.List;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;

public class NpvCalculator {

    /**
     * NPV = sum of cashFlows[t] / (1 + rate)^t for t = 0..n
     * cashFlows[0] is typically the initial outflow (negative value for investments).
     */
    public BigDecimal calculate(List<BigDecimal> cashFlows, BigDecimal rate) {
        BigDecimal npv = BigDecimal.ZERO;
        BigDecimal divisor = BigDecimal.ONE;
        BigDecimal factor = BigDecimal.ONE.add(rate, MC);

        for (BigDecimal cashFlow : cashFlows) {
            npv = npv.add(cashFlow.divide(divisor, MC), MC);
            divisor = divisor.multiply(factor, MC);
        }
        return npv;
    }

    /**
     * Derivative of NPV with respect to rate.
     * dNPV/dr = sum of -t * cashFlows[t] / (1 + rate)^(t+1) for t = 1..n
     */
    public BigDecimal derivative(List<BigDecimal> cashFlows, BigDecimal rate) {
        BigDecimal result = BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE.add(rate, MC);
        BigDecimal divisor = factor;

        for (int t = 1; t < cashFlows.size(); t++) {
            BigDecimal term = cashFlows.get(t)
                    .multiply(BigDecimal.valueOf(-t), MC)
                    .divide(divisor.multiply(factor, MC), MC);
            result = result.add(term, MC);
            divisor = divisor.multiply(factor, MC);
        }
        return result;
    }
}
