package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;

/**
 * One holerite/TRCT row.
 *
 * @param reference quantity shown next to the item ("30 dias", "7/12 avos"); may be null
 * @param taxable   for earnings, whether INSS/IRRF apply; null for deductions
 */
public record PayslipLine(String description, String reference, Type type, BigDecimal amount, Boolean taxable) {

    public enum Type { EARNING, DEDUCTION }

    public static PayslipLine earning(String description, String reference, BigDecimal amount, boolean taxable) {
        return new PayslipLine(description, reference, Type.EARNING, amount, taxable);
    }

    public static PayslipLine deduction(String description, String reference, BigDecimal amount) {
        return new PayslipLine(description, reference, Type.DEDUCTION, amount, null);
    }
}
