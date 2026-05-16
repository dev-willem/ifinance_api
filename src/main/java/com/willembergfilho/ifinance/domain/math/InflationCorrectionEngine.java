package com.willembergfilho.ifinance.domain.math;

import com.willembergfilho.ifinance.domain.simulation.Installment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.*;

// CM stored in additionalCharges; last period settles remaining balance
public class InflationCorrectionEngine {

    public List<Installment> applyCorrection(
            List<Installment> baseInstallments,
            BigDecimal periodicRate,
            List<BigDecimal> periodInflationRates
    ) {
        if (baseInstallments == null || baseInstallments.isEmpty()) {
            throw new CalculationException("Base installments must not be empty.");
        }
        if (periodicRate == null || periodicRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Periodic rate must be non-negative.");
        }

        int terms = baseInstallments.size();
        List<Installment> schedule = new ArrayList<>(terms);
        BigDecimal balance = baseInstallments.getFirst().principalBalanceBefore();

        BigDecimal lastKnownRate = resolveInflation(periodInflationRates, 0);

        for (int period = 1; period <= terms; period++) {
            BigDecimal inflation = resolveInflation(periodInflationRates, period - 1);
            if (inflation.compareTo(BigDecimal.ZERO) >= 0) {
                lastKnownRate = inflation;
            }

            BigDecimal cm = round(balance.multiply(lastKnownRate, MC));
            BigDecimal correctedBalance = round(balance.add(cm, MC));

            BigDecimal interest = round(correctedBalance.multiply(periodicRate, MC));

            int remaining = terms - period + 1;
            BigDecimal amortization;
            BigDecimal balanceAfter;

            if (period == terms) {
                amortization = correctedBalance;
                balanceAfter = BigDecimal.ZERO;
            } else {
                amortization = round(correctedBalance.divide(BigDecimal.valueOf(remaining), MC));
                balanceAfter = round(correctedBalance.subtract(amortization, MC));
            }

            BigDecimal total = round(amortization.add(interest, MC));

            schedule.add(new Installment(
                    period,
                    correctedBalance,
                    amortization,
                    interest,
                    cm,
                    total,
                    balanceAfter
            ));

            balance = balanceAfter;
        }

        return schedule;
    }

    private BigDecimal resolveInflation(List<BigDecimal> rates, int index) {
        if (rates == null || rates.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (index >= rates.size()) {
            return rates.getLast();
        }
        BigDecimal rate = rates.get(index);
        return rate != null ? rate : rates.getLast();
    }
}
