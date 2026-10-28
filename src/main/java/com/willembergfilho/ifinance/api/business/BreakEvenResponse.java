package com.willembergfilho.ifinance.api.business;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.business.BreakEvenResult;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BreakEvenResponse(
        BigDecimal unitContributionMargin,
        BigDecimal contributionMarginRatio,
        BigDecimal accountingBreakEvenUnitsExact,
        BigDecimal accountingBreakEvenUnits,
        BigDecimal accountingBreakEvenRevenue,
        BigDecimal economicBreakEvenUnitsExact,
        BigDecimal economicBreakEvenUnits,
        BigDecimal economicBreakEvenRevenue,
        BigDecimal financialBreakEvenUnitsExact,
        BigDecimal financialBreakEvenUnits,
        BigDecimal financialBreakEvenRevenue,
        BigDecimal currentRevenue,
        BigDecimal operatingProfit,
        BigDecimal safetyMarginUnits,
        BigDecimal safetyMarginRevenue,
        BigDecimal safetyMarginRatio,
        BigDecimal operatingLeverageDegree
) {
    public static BreakEvenResponse from(BreakEvenResult r) {
        return new BreakEvenResponse(
                r.unitContributionMargin(), r.contributionMarginRatio(),
                r.accountingBreakEvenUnitsExact(), r.accountingBreakEvenUnits(), r.accountingBreakEvenRevenue(),
                r.economicBreakEvenUnitsExact(), r.economicBreakEvenUnits(), r.economicBreakEvenRevenue(),
                r.financialBreakEvenUnitsExact(), r.financialBreakEvenUnits(), r.financialBreakEvenRevenue(),
                r.currentRevenue(), r.operatingProfit(),
                r.safetyMarginUnits(), r.safetyMarginRevenue(), r.safetyMarginRatio(),
                r.operatingLeverageDegree()
        );
    }
}
