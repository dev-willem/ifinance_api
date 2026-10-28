package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;

/**
 * @param taxableIncome      gross taxable income; also the reference for the Lei 15.270/2025 reduction
 * @param legalDeductions    INSS + dependents + alimony
 * @param deductionApplied   the larger of legal deductions and the simplified discount
 * @param bracketDeduction   "parcela a deduzir" of the bracket
 */
public record IrrfResult(
        BigDecimal taxableIncome,
        BigDecimal inssDeduction,
        BigDecimal dependentsDeduction,
        BigDecimal alimonyDeduction,
        BigDecimal legalDeductions,
        BigDecimal simplifiedDiscount,
        DeductionMethod deductionMethod,
        BigDecimal deductionApplied,
        BigDecimal base,
        BigDecimal rate,
        BigDecimal bracketDeduction,
        BigDecimal taxBeforeReduction,
        BigDecimal reduction,
        BigDecimal tax,
        BigDecimal effectiveRate
) {
    public enum DeductionMethod { LEGAL, SIMPLIFIED }
}
