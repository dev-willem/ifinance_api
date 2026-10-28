package com.willembergfilho.ifinance.domain.business;

import java.math.BigDecimal;

import static com.willembergfilho.ifinance.domain.business.Ratios.*;

/**
 * One period (month) of a simplified Brazilian DRE. Expenses are positive amounts;
 * salesDeductionsRate is a fraction of gross revenue (taxes on sales, returns, rebates).
 * projectionMonths null = no projection.
 */
public record IncomeStatementParameters(
        BigDecimal grossRevenue,
        BigDecimal salesDeductionsRate,
        BigDecimal costOfGoodsSold,
        BigDecimal sellingExpenses,
        BigDecimal administrativeExpenses,
        BigDecimal otherOperatingExpenses,
        BigDecimal depreciationAmortization,
        BigDecimal financialRevenue,
        BigDecimal financialExpenses,
        BigDecimal incomeTaxRate,
        Integer projectionMonths,
        BigDecimal monthlyRevenueGrowth
) {
    // Teto no Lucro Real (vigente em 2026): IRPJ 15% + CSLL 9% (Lei 7.689/1988) sobre todo o LAIR, mais adicional de 10%
    // só sobre o que exceder R$ 20 mil/mês (Lei 9.249/1995, art. 3º, §1º). Flat 34% overstates tax below that threshold.
    public static final BigDecimal DEFAULT_INCOME_TAX_RATE = new BigDecimal("0.34");
    public static final int MAX_PROJECTION_MONTHS = 60;

    public IncomeStatementParameters {
        salesDeductionsRate = orZero(salesDeductionsRate);
        sellingExpenses = orZero(sellingExpenses);
        administrativeExpenses = orZero(administrativeExpenses);
        otherOperatingExpenses = orZero(otherOperatingExpenses);
        depreciationAmortization = orZero(depreciationAmortization);
        financialRevenue = orZero(financialRevenue);
        financialExpenses = orZero(financialExpenses);
        incomeTaxRate = incomeTaxRate != null ? incomeTaxRate : DEFAULT_INCOME_TAX_RATE;
        monthlyRevenueGrowth = orZero(monthlyRevenueGrowth);

        requireNonNegative(grossRevenue, "A receita bruta");
        requireRate(salesDeductionsRate, "O percentual de deduções");
        requireNonNegative(costOfGoodsSold, "O CMV/CPV/CSP");
        requireNonNegative(sellingExpenses, "As despesas com vendas");
        requireNonNegative(administrativeExpenses, "As despesas administrativas");
        requireNonNegative(otherOperatingExpenses, "As outras despesas operacionais");
        requireNonNegative(depreciationAmortization, "A depreciação e amortização");
        requireNonNegative(financialRevenue, "A receita financeira");
        requireNonNegative(financialExpenses, "A despesa financeira");
        requireRate(incomeTaxRate, "A alíquota de IR/CSLL");
        if (projectionMonths != null && (projectionMonths < 1 || projectionMonths > MAX_PROJECTION_MONTHS)) {
            throw new IllegalArgumentException("A projeção deve ter entre 1 e " + MAX_PROJECTION_MONTHS + " meses.");
        }
        if (monthlyRevenueGrowth.compareTo(BigDecimal.ONE.negate()) <= 0
                || monthlyRevenueGrowth.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("O crescimento mensal da receita deve ser maior que -100% e de no máximo 100%.");
        }
    }
}
