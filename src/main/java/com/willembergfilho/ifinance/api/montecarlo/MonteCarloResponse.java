package com.willembergfilho.ifinance.api.montecarlo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.math.MonetaryRounding;
import com.willembergfilho.ifinance.domain.montecarlo.MonteCarloResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MonteCarloResponse(
        BigDecimal totalContributed,
        BigDecimal expectedFinalValue,
        BigDecimal mean,
        Percentiles percentiles,
        List<YearBand> yearlyBands,
        BigDecimal nominalTarget,
        Double probabilityOfReachingTarget,
        double probabilityBelowContributed,
        List<HistogramBin> histogram,
        int histogramOverflowCount,
        int iterations,
        long seed
) {
    public record Percentiles(BigDecimal p5, BigDecimal p10, BigDecimal p25, BigDecimal p50,
                              BigDecimal p75, BigDecimal p90, BigDecimal p95) {}

    public record YearBand(int year, BigDecimal p5, BigDecimal p25, BigDecimal p50, BigDecimal p75,
                           BigDecimal p95, BigDecimal expected, BigDecimal contributed) {}

    public record HistogramBin(BigDecimal lower, BigDecimal upper, int count) {}

    private static BigDecimal money(double v) {
        return MonetaryRounding.round(BigDecimal.valueOf(v));
    }

    private static double prob(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    public static MonteCarloResponse from(MonteCarloResult r) {
        MonteCarloResult.Percentiles p = r.finalPercentiles();
        return new MonteCarloResponse(
                money(r.totalContributed()),
                money(r.expectedFinalValue()),
                money(r.mean()),
                new Percentiles(money(p.p5()), money(p.p10()), money(p.p25()), money(p.p50()),
                        money(p.p75()), money(p.p90()), money(p.p95())),
                r.yearlyBands().stream()
                        .map(b -> new YearBand(b.year(), money(b.p5()), money(b.p25()), money(b.p50()),
                                money(b.p75()), money(b.p95()), money(b.expected()), money(b.contributed())))
                        .toList(),
                r.nominalTarget() != null ? money(r.nominalTarget()) : null,
                r.probabilityOfReachingTarget() != null ? prob(r.probabilityOfReachingTarget()) : null,
                prob(r.probabilityBelowContributed()),
                r.histogram().stream()
                        .map(h -> new HistogramBin(money(h.lower()), money(h.upper()), h.count()))
                        .toList(),
                r.histogramOverflowCount(),
                r.iterations(),
                r.seed()
        );
    }
}
