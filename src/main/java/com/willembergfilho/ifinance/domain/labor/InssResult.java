package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;
import java.util.List;

/** Employee INSS split by progressive bracket. {@code total} rounds the unrounded sum, as the official ceiling (R$ 988,09) does. */
public record InssResult(
        BigDecimal contributionBase,
        List<BracketShare> brackets,
        BigDecimal total,
        BigDecimal effectiveRate
) {
    public record BracketShare(BigDecimal from, BigDecimal upTo, BigDecimal rate,
                               BigDecimal taxedAmount, BigDecimal contribution) {}
}
