package com.willembergfilho.ifinance.domain.risk;

import java.util.List;

/**
 * Return/risk figures are decimals (0.10 = 10%). Nullable fields are undefined for the series
 * (e.g. zero volatility, no drawdown, no benchmark). Drawdown indices refer to the wealth index,
 * where 0 is the starting point (wealth = 1) and i is the value after the i-th return.
 */
public record RiskMetricsResult(
        int periods,
        int periodsPerYear,
        double cumulativeReturn,
        double annualizedReturn,
        double meanPeriodReturn,
        double periodVolatility,
        double annualizedVolatility,
        double riskFreePeriodRate,
        Double sharpeRatio,
        Double downsideDeviationAnnualized,
        Double sortinoRatio,
        double maxDrawdown,
        Integer drawdownPeakIndex,
        Integer drawdownTroughIndex,
        Integer drawdownRecoveryIndex,
        Double calmarRatio,
        double confidenceLevel,
        double historicalVar,
        double historicalCvar,
        BenchmarkMetrics benchmark,
        List<Double> wealthIndex,
        List<Double> drawdownSeries,
        List<Double> benchmarkWealthIndex
) {

    public record BenchmarkMetrics(
            double cumulativeReturn,
            double annualizedReturn,
            double annualizedVolatility,
            Double beta,
            Double jensensAlphaAnnualized,
            Double correlation,
            Double rSquared,
            double trackingErrorAnnualized,
            Double informationRatio
    ) {}
}
