package com.willembergfilho.ifinance.domain.math;

import java.math.BigDecimal;
import java.util.List;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;

public class PaybackCalculator {

    public record PaybackResult(
            boolean paybackAchieved,
            BigDecimal simplePayback,
            Integer simplePaybackPeriod,
            BigDecimal discountedPayback,
            Integer discountedPaybackPeriod
    ) {}

    /**
     * cashFlows[0] = initial investment (negative).
     * cashFlows[1..n] = periodic cash inflows.
     * discountRate = per-period decimal rate (e.g. 0.10 for 10%); null → skip discounted payback.
     */
    public PaybackResult calculate(List<BigDecimal> cashFlows, BigDecimal discountRate) {
        Info simple = compute(cashFlows, null);
        Info disc   = discountRate != null ? compute(cashFlows, discountRate)
                                           : new Info(false, null, null);
        return new PaybackResult(
                simple.achieved(), simple.exact(), simple.period(),
                disc.exact(), disc.period());
    }

    private record Info(boolean achieved, BigDecimal exact, Integer period) {}

    private Info compute(List<BigDecimal> cashFlows, BigDecimal discountRate) {
        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal factor     = BigDecimal.ONE;
        BigDecimal factorMult = discountRate != null
                ? BigDecimal.ONE.add(discountRate, MC) : BigDecimal.ONE;

        for (int t = 0; t < cashFlows.size(); t++) {
            BigDecimal prev = cumulative;
            BigDecimal cf   = cashFlows.get(t).divide(factor, MC);
            cumulative      = cumulative.add(cf, MC);

            if (cumulative.compareTo(BigDecimal.ZERO) >= 0) {
                if (t == 0 || prev.compareTo(BigDecimal.ZERO) >= 0) {
                    return new Info(true, BigDecimal.valueOf(t), t);
                }
                BigDecimal fraction = cf.compareTo(BigDecimal.ZERO) != 0
                        ? prev.negate().divide(cf, MC) : BigDecimal.ZERO;
                BigDecimal exact = MonetaryRounding.round(
                        BigDecimal.valueOf(t - 1).add(fraction, MC).max(BigDecimal.ZERO));
                return new Info(true, exact, t);
            }

            factor = factor.multiply(factorMult, MC);
        }
        return new Info(false, null, null);
    }
}
