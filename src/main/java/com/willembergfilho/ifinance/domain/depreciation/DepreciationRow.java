package com.willembergfilho.ifinance.domain.depreciation;

import java.math.BigDecimal;

public record DepreciationRow(
        int year,
        int months,
        BigDecimal openingBookValue,
        BigDecimal depreciation,
        BigDecimal accumulatedDepreciation,
        BigDecimal closingBookValue
) {}
