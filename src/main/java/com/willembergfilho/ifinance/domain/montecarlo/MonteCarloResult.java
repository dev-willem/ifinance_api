package com.willembergfilho.ifinance.domain.montecarlo;

import java.util.List;

public record MonteCarloResult(
        double totalContributed,
        double expectedFinalValue,
        double mean,
        Percentiles finalPercentiles,
        List<YearBand> yearlyBands,
        Double nominalTarget,
        Double probabilityOfReachingTarget,
        double probabilityBelowContributed,
        List<HistogramBin> histogram,
        int histogramOverflowCount,
        int iterations,
        long seed
) {
    public record Percentiles(double p5, double p10, double p25, double p50, double p75, double p90, double p95) {}

    /** {@code expected} is the deterministic projection at the expected return; {@code contributed} is cumulative. */
    public record YearBand(int year, double p5, double p25, double p50, double p75, double p95,
                           double expected, double contributed) {}

    public record HistogramBin(double lower, double upper, int count) {}
}
