package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payroll tax parameters in force for one calendar year.
 *
 * @param irrfReduction monthly IRRF reduction (Lei 15.270/2025); null for years without it
 */
public record LaborTaxTable(
        int year,
        BigDecimal minimumWage,
        List<InssBracket> inssBrackets,
        List<IrrfBracket> irrfBrackets,
        BigDecimal dependentDeduction,
        BigDecimal simplifiedDiscount,
        IrrfReduction irrfReduction
) {
    public LaborTaxTable {
        inssBrackets = List.copyOf(inssBrackets);
        irrfBrackets = List.copyOf(irrfBrackets);
    }

    /** Employee INSS bracket: the rate applies to the slice of salary up to {@code upTo}. */
    public record InssBracket(BigDecimal upTo, BigDecimal rate) {}

    /** IRRF monthly bracket; {@code upTo == null} marks the open top bracket. */
    public record IrrfBracket(BigDecimal upTo, BigDecimal rate, BigDecimal deduction) {}

    /**
     * Up to {@code fullUpTo} of taxable income the tax is reduced by up to {@code maxReduction} (to zero);
     * up to {@code phaseOutUpTo} the reduction is {@code constant - slope x income}.
     */
    public record IrrfReduction(BigDecimal fullUpTo, BigDecimal maxReduction,
                                BigDecimal phaseOutUpTo, BigDecimal constant, BigDecimal slope) {}

    public BigDecimal inssCeiling() {
        return inssBrackets.getLast().upTo();
    }
}
