package com.willembergfilho.ifinance.domain.tax;

import java.math.BigDecimal;

/** {@code share} is the fraction of the total paid (DAS or regime total) that goes to {@code tax}. */
public record TaxShare(
        TaxType tax,
        BigDecimal share,
        BigDecimal amount
) {}
