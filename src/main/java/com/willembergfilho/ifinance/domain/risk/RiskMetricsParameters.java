package com.willembergfilho.ifinance.domain.risk;

import java.util.List;

/**
 * @param benchmarkValues optional; same input type and length as {@code values}
 */
public record RiskMetricsParameters(
        List<Double> values,
        RiskInputType inputType,
        List<Double> benchmarkValues,
        Periodicity periodicity,
        double riskFreeAnnualRate,
        double confidenceLevel
) {}
