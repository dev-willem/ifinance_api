package com.willembergfilho.ifinance.domain.tax;

import java.math.BigDecimal;
import java.util.List;

/** Annual amounts. When {@code eligible} is false the breakdown is empty and the totals are null. */
public record RegimeResult(
        TaxRegime regime,
        boolean eligible,
        List<TaxShare> breakdown,
        BigDecimal total,
        BigDecimal effectiveRate,
        BigDecimal netProfit,
        List<String> notes
) {}
