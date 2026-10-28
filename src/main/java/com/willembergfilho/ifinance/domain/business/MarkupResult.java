package com.willembergfilho.ifinance.domain.business;

import java.math.BigDecimal;
import java.util.List;

/** Composition amounts add up exactly to suggestedPrice; actual* fields are null without currentPrice. */
public record MarkupResult(
        BigDecimal totalRate,
        BigDecimal markupDivisor,
        BigDecimal markupMultiplier,
        BigDecimal suggestedPrice,
        List<PriceComponent> composition,
        BigDecimal currentPrice,
        BigDecimal actualProfitAmount,
        BigDecimal actualProfitMargin
) {
    public record PriceComponent(String key, String label, BigDecimal rate, BigDecimal amount) {}
}
