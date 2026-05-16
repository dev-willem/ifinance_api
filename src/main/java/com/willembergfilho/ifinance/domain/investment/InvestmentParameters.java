package com.willembergfilho.ifinance.domain.investment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentParameters(
        String name,
        InvestmentType type,
        RateBasis rateBasis,
        BigDecimal rateValue,
        BigDecimal principal,
        int termDays,
        LocalDate startDate
) {}
