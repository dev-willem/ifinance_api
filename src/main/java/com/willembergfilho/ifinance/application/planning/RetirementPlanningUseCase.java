package com.willembergfilho.ifinance.application.planning;

import com.willembergfilho.ifinance.domain.math.MonetaryRounding;
import com.willembergfilho.ifinance.domain.planning.RetirementParameters;
import com.willembergfilho.ifinance.domain.planning.RetirementResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

@Slf4j
@Service
public class RetirementPlanningUseCase {

    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal TWELVE = new BigDecimal("12");

    public RetirementResult execute(RetirementParameters p) {
        MathContext mc = MonetaryRounding.MC;

        BigDecimal annualExpenses = p.monthlyExpenses().multiply(TWELVE, mc);
        BigDecimal targetAmount = MonetaryRounding.round(annualExpenses.divide(p.withdrawalRate(), mc));

        // Monthly return rate: (1 + annual)^(1/12) - 1
        double annualReturnDouble = p.expectedAnnualReturn().doubleValue();
        double monthlyRateDouble = Math.pow(1 + annualReturnDouble, 1.0 / 12) - 1;
        BigDecimal monthlyRate = MonetaryRounding.roundRate(BigDecimal.valueOf(monthlyRateDouble));

        boolean alreadyFire = p.currentSavings().compareTo(targetAmount) >= 0;

        Integer monthsToFire = null;
        Integer yearsToFire = null;
        BigDecimal remainingMonths = null;

        if (!alreadyFire) {
            double C = p.currentSavings().doubleValue();
            double S = p.monthlySavings().doubleValue();
            double r = monthlyRateDouble;
            double T = targetAmount.doubleValue();

            double n;
            if (r < 1e-10) {
                // No return — simple linear: months = (T - C) / S
                n = S > 0 ? (T - C) / S : Double.POSITIVE_INFINITY;
            } else {
                // FV = C*(1+r)^n + S*((1+r)^n - 1)/r = T
                // (C*r + S) * (1+r)^n = T*r + S
                // n = log((T*r + S) / (C*r + S)) / log(1 + r)
                double numerator = T * r + S;
                double denominator = C * r + S;
                if (denominator <= 0 || numerator <= 0) {
                    n = Double.POSITIVE_INFINITY;
                } else {
                    n = Math.log(numerator / denominator) / Math.log(1 + r);
                }
            }

            if (!Double.isInfinite(n) && !Double.isNaN(n) && n >= 0) {
                monthsToFire = (int) Math.ceil(n);
                yearsToFire = monthsToFire / 12;
                remainingMonths = BigDecimal.valueOf(n);
            }
        }

        // Project savings at 10 and 20 years regardless
        BigDecimal proj10 = projectSavings(p.currentSavings(), p.monthlySavings(), monthlyRateDouble, 120, mc);
        BigDecimal proj20 = projectSavings(p.currentSavings(), p.monthlySavings(), monthlyRateDouble, 240, mc);

        return new RetirementResult(
                targetAmount,
                MonetaryRounding.round(annualExpenses),
                monthlyRate,
                monthsToFire,
                yearsToFire,
                remainingMonths,
                alreadyFire,
                proj10,
                proj20
        );
    }

    private BigDecimal projectSavings(BigDecimal current, BigDecimal monthly, double r, int months, MathContext mc) {
        double C = current.doubleValue();
        double S = monthly.doubleValue();
        double fv;
        if (r < 1e-10) {
            fv = C + S * months;
        } else {
            double factor = Math.pow(1 + r, months);
            fv = C * factor + S * (factor - 1) / r;
        }
        return MonetaryRounding.round(BigDecimal.valueOf(fv));
    }
}
