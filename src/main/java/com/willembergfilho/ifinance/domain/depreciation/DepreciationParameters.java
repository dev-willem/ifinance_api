package com.willembergfilho.ifinance.domain.depreciation;

import java.math.BigDecimal;
import java.util.List;

public record DepreciationParameters(
        BigDecimal assetValue,
        BigDecimal residualValue,
        int usefulLifeYears,
        DepreciationMethod method,
        BigDecimal decliningFactor,
        BigDecimal totalUnits,
        List<BigDecimal> unitsPerYear,
        int shiftsPerDay,
        int firstYearMonths
) {
    public static final BigDecimal DEFAULT_DECLINING_FACTOR = new BigDecimal("2.0");

    public DepreciationParameters {
        if (decliningFactor == null) decliningFactor = DEFAULT_DECLINING_FACTOR;
        unitsPerYear = unitsPerYear == null ? List.of() : List.copyOf(unitsPerYear);
    }

    public DepreciationParameters withMethod(DepreciationMethod m) {
        return new DepreciationParameters(assetValue, residualValue, usefulLifeYears, m, decliningFactor,
                totalUnits, unitsPerYear, shiftsPerDay, firstYearMonths);
    }
}
