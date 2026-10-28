package com.willembergfilho.ifinance.domain.tax;

import java.util.List;

public record TaxComparisonResult(
        List<RegimeResult> regimes,
        TaxRegime cheapest,
        String disclaimer
) {}
