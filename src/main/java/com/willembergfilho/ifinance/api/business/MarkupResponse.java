package com.willembergfilho.ifinance.api.business;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.business.MarkupResult;
import com.willembergfilho.ifinance.domain.business.MarkupResult.PriceComponent;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MarkupResponse(
        BigDecimal totalRate,
        BigDecimal markupDivisor,
        BigDecimal markupMultiplier,
        BigDecimal suggestedPrice,
        List<PriceComponent> composition,
        BigDecimal currentPrice,
        BigDecimal actualProfitAmount,
        BigDecimal actualProfitMargin
) {
    public static MarkupResponse from(MarkupResult r) {
        return new MarkupResponse(
                r.totalRate(), r.markupDivisor(), r.markupMultiplier(), r.suggestedPrice(), r.composition(),
                r.currentPrice(), r.actualProfitAmount(), r.actualProfitMargin()
        );
    }
}
