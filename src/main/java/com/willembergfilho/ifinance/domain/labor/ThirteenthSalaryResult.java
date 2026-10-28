package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param firstInstallment  50% advance, paid without deductions
 * @param secondInstallment remaining gross minus INSS and IRRF computed on the full 13º
 */
public record ThirteenthSalaryResult(
        int year,
        BigDecimal remuneration,
        int monthsWorked,
        BigDecimal grossTotal,
        BigDecimal firstInstallment,
        BigDecimal secondInstallmentGross,
        InssResult inss,
        IrrfResult irrf,
        BigDecimal secondInstallment,
        BigDecimal totalDeductions,
        BigDecimal netTotal,
        List<PayslipLine> lines
) {}
