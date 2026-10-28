package com.willembergfilho.ifinance.api.business;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.business.IncomeStatementResult;
import com.willembergfilho.ifinance.domain.business.IncomeStatementResult.ProjectionMonth;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IncomeStatementResponse(
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
    public static IncomeStatementResponse from(IncomeStatementResult r) {
        return new IncomeStatementResponse(
                r.grossRevenue(), r.salesDeductions(), r.netRevenue(), r.costOfGoodsSold(), r.grossProfit(),
                r.sellingExpenses(), r.administrativeExpenses(), r.otherOperatingExpenses(), r.operatingExpenses(),
                r.ebitda(), r.depreciationAmortization(), r.ebit(),
                r.financialRevenue(), r.financialExpenses(), r.financialResult(),
                r.earningsBeforeTax(), r.incomeTax(), r.netIncome(),
                r.grossMargin(), r.ebitdaMargin(), r.operatingMargin(), r.netMargin(),
                r.projection()
        );
    }
}
