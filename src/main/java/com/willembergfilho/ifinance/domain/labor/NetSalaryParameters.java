package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;

import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.orZero;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.requireNonNegative;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.requirePositive;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.requireRange;

/**
 * @param alimony          court-ordered alimony (pensão alimentícia judicial): discounted and deductible from the IRRF base
 * @param otherDiscounts   plain payroll discounts (vale-transporte, plano de saúde...): discounted, not deductible
 * @param employerInssRate employer social security rate (patronal + RAT + terceiros); default 20%
 * @param year             tax table year; null = latest
 */
public record NetSalaryParameters(
        BigDecimal grossSalary,
        int dependents,
        BigDecimal alimony,
        BigDecimal otherDiscounts,
        BigDecimal employerInssRate,
        Integer year
) {
    public static final BigDecimal DEFAULT_EMPLOYER_INSS_RATE = new BigDecimal("0.20");

    public NetSalaryParameters {
        requirePositive(grossSalary, "Salário bruto");
        requireRange(dependents, 0, 30, "Número de dependentes");
        alimony = orZero(alimony);
        otherDiscounts = orZero(otherDiscounts);
        requireNonNegative(alimony, "Pensão alimentícia");
        requireNonNegative(otherDiscounts, "Outros descontos");
        employerInssRate = employerInssRate == null ? DEFAULT_EMPLOYER_INSS_RATE : employerInssRate;
        if (employerInssRate.signum() < 0 || employerInssRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Alíquota patronal deve estar entre 0% e 100%.");
        }
    }
}
