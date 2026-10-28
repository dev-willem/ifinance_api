package com.willembergfilho.ifinance.application.business;

import com.willembergfilho.ifinance.domain.business.IncomeStatementParameters;
import com.willembergfilho.ifinance.domain.business.IncomeStatementResult;
import com.willembergfilho.ifinance.domain.business.IncomeStatementResult.ProjectionMonth;
import com.willembergfilho.ifinance.domain.business.Ratios;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.round;

@Service
public class IncomeStatementUseCase {

    public IncomeStatementResult execute(IncomeStatementParameters p) {
        List<ProjectionMonth> projection = null;
        if (p.projectionMonths() != null) {
            projection = new ArrayList<>(p.projectionMonths());
            BigDecimal growth = BigDecimal.ONE.add(p.monthlyRevenueGrowth());
            BigDecimal factor = BigDecimal.ONE;
            BigDecimal cumulative = BigDecimal.ZERO.setScale(2);
            for (int month = 1; month <= p.projectionMonths(); month++) {
                IncomeStatementResult s = statement(p, factor, null);
                cumulative = cumulative.add(s.netIncome());
                projection.add(new ProjectionMonth(month, s.grossRevenue(), s.netRevenue(), s.grossProfit(),
                        s.ebitda(), s.ebit(), s.earningsBeforeTax(), s.incomeTax(), s.netIncome(),
                        s.netMargin(), cumulative));
                factor = factor.multiply(growth, MC);
            }
        }
        return statement(p, BigDecimal.ONE, projection);
    }

    /**
     * revenueFactor scales the variable lines (gross revenue, deductions, COGS, selling expenses);
     * administrative/other expenses, D&A and financial result stay constant. Each line is rounded
     * before subtotals so the statement always adds up to the cent.
     */
    private IncomeStatementResult statement(IncomeStatementParameters p, BigDecimal revenueFactor,
                                            List<ProjectionMonth> projection) {
        BigDecimal grossRevenue = round(p.grossRevenue().multiply(revenueFactor, MC));
        BigDecimal deductions = round(grossRevenue.multiply(p.salesDeductionsRate(), MC));
        BigDecimal netRevenue = grossRevenue.subtract(deductions);
        BigDecimal cogs = round(p.costOfGoodsSold().multiply(revenueFactor, MC));
        BigDecimal grossProfit = netRevenue.subtract(cogs);

        BigDecimal selling = round(p.sellingExpenses().multiply(revenueFactor, MC));
        BigDecimal administrative = round(p.administrativeExpenses());
        BigDecimal other = round(p.otherOperatingExpenses());
        BigDecimal operatingExpenses = selling.add(administrative).add(other);
        BigDecimal ebitda = grossProfit.subtract(operatingExpenses);

        BigDecimal da = round(p.depreciationAmortization());
        BigDecimal ebit = ebitda.subtract(da);

        BigDecimal financialRevenue = round(p.financialRevenue());
        BigDecimal financialExpenses = round(p.financialExpenses());
        BigDecimal financialResult = financialRevenue.subtract(financialExpenses);
        BigDecimal earningsBeforeTax = ebit.add(financialResult);

        // A loss (LAIR <= 0) pays no IRPJ/CSLL; loss carry-forward between months is not modelled.
        BigDecimal incomeTax = earningsBeforeTax.signum() > 0
                ? round(earningsBeforeTax.multiply(p.incomeTaxRate(), MC))
                : BigDecimal.ZERO.setScale(2);
        BigDecimal netIncome = earningsBeforeTax.subtract(incomeTax);

        return new IncomeStatementResult(
                grossRevenue, deductions, netRevenue, cogs, grossProfit,
                selling, administrative, other, operatingExpenses, ebitda,
                da, ebit, financialRevenue, financialExpenses, financialResult,
                earningsBeforeTax, incomeTax, netIncome,
                Ratios.of(grossProfit, netRevenue),
                Ratios.of(ebitda, netRevenue),
                Ratios.of(ebit, netRevenue),
                Ratios.of(netIncome, netRevenue),
                projection
        );
    }
}
