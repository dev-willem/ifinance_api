package com.willembergfilho.ifinance.domain.credit;

import java.math.BigDecimal;

/**
 * Effective rates measure what the borrower actually pays for receiving presentValue now
 * and paying faceValue after n periods:
 * total = N/A - 1; simple per period = total/n; compound per period = (N/A)^(1/n) - 1.
 * Numeric fields are null when valid is false.
 */
public record DiscountResult(
        DiscountModality modality,
        boolean valid,
        BigDecimal presentValue,
        BigDecimal discountAmount,
        BigDecimal effectiveRateTotal,
        BigDecimal effectiveSimpleRatePerPeriod,
        BigDecimal effectiveCompoundRatePerPeriod,
        String message
) {
    public static DiscountResult invalid(DiscountModality modality, String message) {
        return new DiscountResult(modality, false, null, null, null, null, null, message);
    }
}
