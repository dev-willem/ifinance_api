package com.willembergfilho.ifinance.domain.business;

import java.math.BigDecimal;
import java.util.List;

/** Margins are fractions of net revenue; null when net revenue is zero. */
public record IncomeStatementResult(
        BigDecimal grossRevenue,
        BigDecimal salesDeductions,
        BigDecimal netRevenue,
        BigDecimal costOfGoodsSold,
        BigDecimal grossProfit,
        BigDecimal sellingExpenses,
        BigDecimal administrativeExpenses,
        BigDecimal otherOperatingExpenses,
        BigDecimal operatingExpenses,
        BigDecimal ebitda,
        BigDecimal depreciationAmortization,
        BigDecimal ebit,
        BigDecimal financialRevenue,
        BigDecimal financialExpenses,
        BigDecimal financialResult,
        BigDecimal earningsBeforeTax,
        BigDecimal incomeTax,
        BigDecimal netIncome,
        BigDecimal grossMargin,
        BigDecimal ebitdaMargin,
        BigDecimal operatingMargin,
        BigDecimal netMargin,
        List<ProjectionMonth> projection
) {
    public record ProjectionMonth(
            int month,
            BigDecimal grossRevenue,
            BigDecimal netRevenue,
            BigDecimal grossProfit,
            BigDecimal ebitda,
            BigDecimal ebit,
            BigDecimal earningsBeforeTax,
            BigDecimal incomeTax,
            BigDecimal netIncome,
            BigDecimal netMargin,
            BigDecimal cumulativeNetIncome
    ) {}
}
