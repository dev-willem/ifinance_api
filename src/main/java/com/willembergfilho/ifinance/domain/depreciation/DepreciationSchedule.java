package com.willembergfilho.ifinance.domain.depreciation;

import java.math.BigDecimal;
import java.util.List;

public record DepreciationSchedule(
        DepreciationMethod method,
        BigDecimal annualRatePercent,
        BigDecimal effectiveLifeYears,
        BigDecimal monthlyDepreciationFirstFullYear,
        BigDecimal depreciableBase,
        BigDecimal totalDepreciation,
        BigDecimal finalBookValue,
        List<DepreciationRow> rows
) {}
