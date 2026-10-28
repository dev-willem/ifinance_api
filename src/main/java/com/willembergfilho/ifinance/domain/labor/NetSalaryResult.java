package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param fgts employer deposit (8%), not discounted from the employee
 */
public record NetSalaryResult(
        int year,
        BigDecimal grossSalary,
        InssResult inss,
        IrrfResult irrf,
        BigDecimal alimony,
        BigDecimal otherDiscounts,
        BigDecimal totalDeductions,
        BigDecimal netSalary,
        BigDecimal fgts,
        BigDecimal totalTaxEffectiveRate,
        EmployerCost employerCost,
        List<PayslipLine> lines
) {
    /**
     * Monthly employer cost: salary, FGTS and employer INSS, plus monthly provisions for the 13º (1/12)
     * and vacation + 1/3 (4/3 / 12), with FGTS and employer INSS also charged on those provisions.
     */
    public record EmployerCost(
            BigDecimal salary,
            BigDecimal fgts,
            BigDecimal employerInssRate,
            BigDecimal employerInss,
            BigDecimal thirteenthProvision,
            BigDecimal vacationProvision,
            BigDecimal provisionCharges,
            BigDecimal total,
            BigDecimal costToSalaryRatio
    ) {}
}
