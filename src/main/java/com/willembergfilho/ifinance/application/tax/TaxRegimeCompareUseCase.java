package com.willembergfilho.ifinance.application.tax;

import com.willembergfilho.ifinance.domain.tax.TaxComparisonParameters;
import com.willembergfilho.ifinance.domain.tax.TaxComparisonResult;
import com.willembergfilho.ifinance.domain.tax.TaxRegimeComparator;
import org.springframework.stereotype.Service;

@Service
public class TaxRegimeCompareUseCase {

    private final TaxRegimeComparator comparator = new TaxRegimeComparator();

    public TaxComparisonResult execute(TaxComparisonParameters params) {
        return comparator.compare(params);
    }
}
