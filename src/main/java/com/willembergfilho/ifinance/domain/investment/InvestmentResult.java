package com.willembergfilho.ifinance.domain.investment;

import java.math.BigDecimal;

public record InvestmentResult(
        BigDecimal grossReturn,
        BigDecimal netReturn,
        BigDecimal irAmount,
        BigDecimal irRate,
        BigDecimal grossAnnualRate,
        BigDecimal netAnnualRate,
        BigDecimal indexRateUsed
) {}
