package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param remuneration salary + average variable pay, the base for vacation pay
 * @param taxableTotal vacation pay + 1/3, the only part subject to INSS/IRRF
 */
public record VacationResult(
        int year,
        BigDecimal remuneration,
        int days,
        int soldDays,
        BigDecimal vacationPay,
        BigDecimal vacationBonus,
        BigDecimal allowance,
        BigDecimal allowanceBonus,
        BigDecimal taxableTotal,
        InssResult inss,
        IrrfResult irrf,
        BigDecimal grossTotal,
        BigDecimal totalDeductions,
        BigDecimal netTotal,
        List<PayslipLine> lines
) {}
