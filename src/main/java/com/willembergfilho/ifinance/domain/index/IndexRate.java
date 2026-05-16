package com.willembergfilho.ifinance.domain.index;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndexRate(
        EconomicIndex index,
        LocalDate referenceDate,
        BigDecimal rate,
        boolean isProjection
) {}
