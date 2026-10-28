package com.willembergfilho.ifinance.domain.credit;

import java.math.BigDecimal;

public record DiscountParameters(
        BigDecimal faceValue,
        BigDecimal ratePerPeriod,
        BigDecimal periods
) {}
