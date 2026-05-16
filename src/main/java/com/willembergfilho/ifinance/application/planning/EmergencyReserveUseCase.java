package com.willembergfilho.ifinance.application.planning;

import com.willembergfilho.ifinance.domain.math.MonetaryRounding;
import com.willembergfilho.ifinance.domain.planning.EmergencyReserveParameters;
import com.willembergfilho.ifinance.domain.planning.EmergencyReserveResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

@Slf4j
@Service
public class EmergencyReserveUseCase {

    private static final BigDecimal ONE = BigDecimal.ONE;

    public EmergencyReserveResult execute(EmergencyReserveParameters p) {
        MathContext mc = MonetaryRounding.MC;

        BigDecimal targetAmount = MonetaryRounding.round(
                p.monthlyExpenses().multiply(BigDecimal.valueOf(p.monthsCoverage()), mc));

        double currentCoverageMonths = p.currentSavings().doubleValue()
                / p.monthlyExpenses().doubleValue();

        double progressPercent = Math.min(100.0,
                p.currentSavings().doubleValue() / targetAmount.doubleValue() * 100);

        boolean targetReached = p.currentSavings().compareTo(targetAmount) >= 0;

        Integer monthsToComplete = null;
        BigDecimal monthlyShortfall = null;

        if (!targetReached) {
            monthlyShortfall = MonetaryRounding.round(
                    targetAmount.subtract(p.currentSavings(), mc));

            double T = targetAmount.doubleValue();
            double C = p.currentSavings().doubleValue();
            double S = p.monthlySavings().doubleValue();
            double annualReturn = p.expectedAnnualReturn() != null
                    ? p.expectedAnnualReturn().doubleValue() : 0;
            double r = annualReturn > 0
                    ? Math.pow(1 + annualReturn, 1.0 / 12) - 1
                    : 0;

            double n;
            if (r < 1e-10) {
                n = S > 0 ? (T - C) / S : Double.POSITIVE_INFINITY;
            } else {
                double numerator = T * r + S;
                double denominator = C * r + S;
                n = (denominator > 0 && numerator > 0)
                        ? Math.log(numerator / denominator) / Math.log(1 + r)
                        : Double.POSITIVE_INFINITY;
            }

            if (!Double.isInfinite(n) && !Double.isNaN(n) && n >= 0) {
                monthsToComplete = (int) Math.ceil(n);
            }
        }

        return new EmergencyReserveResult(
                targetAmount,
                p.currentSavings(),
                currentCoverageMonths,
                progressPercent,
                targetReached,
                monthsToComplete,
                monthlyShortfall
        );
    }
}
