package com.willembergfilho.ifinance.api.depreciation;

import com.willembergfilho.ifinance.domain.depreciation.DepreciationMethod;
import com.willembergfilho.ifinance.domain.depreciation.DepreciationRow;
import com.willembergfilho.ifinance.domain.depreciation.DepreciationSchedule;

import java.math.BigDecimal;
import java.util.List;

public record DepreciationResponse(
        DepreciationMethod method,
        BigDecimal annualRatePercent,
        BigDecimal effectiveLifeYears,
        BigDecimal monthlyDepreciationFirstFullYear,
        BigDecimal depreciableBase,
        BigDecimal totalDepreciation,
        BigDecimal finalBookValue,
        List<DepreciationRow> rows
) {
    public static DepreciationResponse from(DepreciationSchedule s) {
        return new DepreciationResponse(
                s.method(),
                s.annualRatePercent(),
                s.effectiveLifeYears(),
                s.monthlyDepreciationFirstFullYear(),
                s.depreciableBase(),
                s.totalDepreciation(),
                s.finalBookValue(),
                s.rows()
        );
    }
}
