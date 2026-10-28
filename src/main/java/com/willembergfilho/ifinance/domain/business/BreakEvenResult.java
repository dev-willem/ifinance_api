package com.willembergfilho.ifinance.domain.business;

import java.math.BigDecimal;

/**
 * *UnitsExact keep the fractional quantity; *Units are rounded up (a fraction of a unit cannot be sold).
 * Economic fields are null without desiredProfit; current/safety/leverage fields are null without currentQuantity.
 * operatingLeverageDegree is also null when operating profit is zero (exactly at break-even).
 */
public record BreakEvenResult(
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
) {}
