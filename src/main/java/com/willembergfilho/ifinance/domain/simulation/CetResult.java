package com.willembergfilho.ifinance.domain.simulation;

import java.math.BigDecimal;
import java.util.List;

public record CetResult(
        BigDecimal monthlyRate,
        BigDecimal annualRate,
        List<BigDecimal> cashFlows
) {}
