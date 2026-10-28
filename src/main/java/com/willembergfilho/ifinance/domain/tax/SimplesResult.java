package com.willembergfilho.ifinance.domain.tax;

import java.math.BigDecimal;
import java.util.List;

/** When {@code eligible} is false the bracket, rate and DAS fields are null. */
public record SimplesResult(
        SimplesAnnex requestedAnnex,
        SimplesAnnex appliedAnnex,
        BigDecimal fatorR,
        boolean eligible,
        boolean aboveSublimit,
        Integer bracket,
        BigDecimal nominalRate,
        BigDecimal deduction,
        BigDecimal effectiveRate,
        BigDecimal dasAmount,
        List<TaxShare> shares,
        List<String> notes
) {}
