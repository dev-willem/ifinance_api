package com.willembergfilho.ifinance.api.tax;

import com.willembergfilho.ifinance.domain.tax.RegimeResult;
import com.willembergfilho.ifinance.domain.tax.TaxComparisonResult;
import com.willembergfilho.ifinance.domain.tax.TaxRegime;

import java.util.List;

public record TaxCompareResponse(
        List<RegimeResult> regimes,
        TaxRegime cheapest,
        String disclaimer
) {
    public static TaxCompareResponse from(TaxComparisonResult r) {
        return new TaxCompareResponse(r.regimes(), r.cheapest(), r.disclaimer());
    }
}
