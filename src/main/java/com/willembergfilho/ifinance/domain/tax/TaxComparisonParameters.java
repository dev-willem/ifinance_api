package com.willembergfilho.ifinance.domain.tax;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNullElse;

/**
 * Annual figures. {@code deductibleExpenses} are all costs and expenses except payroll and taxes;
 * {@code creditablePurchases} is the part of them that generates PIS/Cofins credits in the non-cumulative regime.
 * {@code salesTaxRate} is the effective ICMS or ISS rate on revenue, paid outside the DAS.
 */
public record TaxComparisonParameters(
        BigDecimal annualRevenue,
        BusinessActivity activity,
        BigDecimal annualPayroll,
        BigDecimal payrollChargesRate,
        BigDecimal deductibleExpenses,
        BigDecimal creditablePurchases,
        BigDecimal salesTaxRate
) {
    public TaxComparisonParameters {
        annualPayroll = requireNonNullElse(annualPayroll, BigDecimal.ZERO);
        payrollChargesRate = requireNonNullElse(payrollChargesRate, TaxRules.DEFAULT_PAYROLL_CHARGES_RATE);
        deductibleExpenses = requireNonNullElse(deductibleExpenses, BigDecimal.ZERO);
        creditablePurchases = requireNonNullElse(creditablePurchases, BigDecimal.ZERO);
        salesTaxRate = requireNonNullElse(salesTaxRate, BigDecimal.ZERO);
    }
}
