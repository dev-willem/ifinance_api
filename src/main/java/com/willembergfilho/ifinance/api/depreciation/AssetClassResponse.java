package com.willembergfilho.ifinance.api.depreciation;

import com.willembergfilho.ifinance.domain.depreciation.AssetClass;

import java.math.BigDecimal;

public record AssetClassResponse(
        String code,
        String description,
        String reference,
        BigDecimal annualRatePercent,
        int usefulLifeYears
) {
    public static AssetClassResponse from(AssetClass c) {
        return new AssetClassResponse(c.name(), c.description(), c.reference(),
                c.annualRatePercent(), c.usefulLifeYears());
    }
}
