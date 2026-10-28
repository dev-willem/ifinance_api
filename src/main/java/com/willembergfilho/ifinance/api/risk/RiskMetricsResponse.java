package com.willembergfilho.ifinance.api.risk;

import com.willembergfilho.ifinance.domain.risk.RiskMetricsResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Decimals (0.10 = 10%). Null means "not defined for this series"; NaN/Infinity never reach JSON.
 * Drawdown indices refer to the wealth index (0 = start, i = after the i-th return).
 */
public record RiskMetricsResponse(
        int periods,
        int periodsPerYear,
        Double cumulativeReturn,
        Double annualizedReturn,
        Double meanPeriodReturn,
        Double periodVolatility,
        Double annualizedVolatility,
        Double riskFreePeriodRate,
        Double sharpeRatio,
        Double downsideDeviationAnnualized,
        Double sortinoRatio,
        Double maxDrawdown,
        Integer drawdownPeakIndex,
        Integer drawdownTroughIndex,
        Integer drawdownRecoveryIndex,
        Double calmarRatio,
        Double confidenceLevel,
        Double historicalVar,
        Double historicalCvar,
        Benchmark benchmark,
        List<Double> wealthIndex,
        List<Double> drawdownSeries,
        List<Double> benchmarkWealthIndex
) {

    public record Benchmark(
            Double cumulativeReturn,
            Double annualizedReturn,
            Double annualizedVolatility,
            Double beta,
            Double jensensAlphaAnnualized,
            Double correlation,
            Double rSquared,
            Double trackingErrorAnnualized,
            Double informationRatio
    ) {}

    private static Double r6(Double v) {
        if (v == null || !Double.isFinite(v)) return null;
        return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }

    private static List<Double> r6(List<Double> xs) {
        // Stream.toList() permits nulls, unlike List.of
        return xs == null ? null : xs.stream().map(RiskMetricsResponse::r6).toList();
    }

    public static RiskMetricsResponse from(RiskMetricsResult r) {
        RiskMetricsResult.BenchmarkMetrics b = r.benchmark();
        return new RiskMetricsResponse(
                r.periods(),
                r.periodsPerYear(),
                r6(r.cumulativeReturn()),
                r6(r.annualizedReturn()),
                r6(r.meanPeriodReturn()),
                r6(r.periodVolatility()),
                r6(r.annualizedVolatility()),
                r6(r.riskFreePeriodRate()),
                r6(r.sharpeRatio()),
                r6(r.downsideDeviationAnnualized()),
                r6(r.sortinoRatio()),
                r6(r.maxDrawdown()),
                r.drawdownPeakIndex(),
                r.drawdownTroughIndex(),
                r.drawdownRecoveryIndex(),
                r6(r.calmarRatio()),
                r6(r.confidenceLevel()),
                r6(r.historicalVar()),
                r6(r.historicalCvar()),
                b == null ? null : new Benchmark(
                        r6(b.cumulativeReturn()),
                        r6(b.annualizedReturn()),
                        r6(b.annualizedVolatility()),
                        r6(b.beta()),
                        r6(b.jensensAlphaAnnualized()),
                        r6(b.correlation()),
                        r6(b.rSquared()),
                        r6(b.trackingErrorAnnualized()),
                        r6(b.informationRatio())),
                r6(r.wealthIndex()),
                r6(r.drawdownSeries()),
                r6(r.benchmarkWealthIndex())
        );
    }
}
