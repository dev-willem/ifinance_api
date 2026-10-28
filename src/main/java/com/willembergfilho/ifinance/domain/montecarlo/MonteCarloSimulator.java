package com.willembergfilho.ifinance.domain.montecarlo;

import com.willembergfilho.ifinance.domain.math.CalculationException;
import com.willembergfilho.ifinance.domain.montecarlo.MonteCarloResult.HistogramBin;
import com.willembergfilho.ifinance.domain.montecarlo.MonteCarloResult.Percentiles;
import com.willembergfilho.ifinance.domain.montecarlo.MonteCarloResult.YearBand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Monthly log-normal return model: ln(1 + r_month) ~ N(m, s²) with s = σ/√12 and
 * m = ln(1 + μ)/12 − s²/2, so E[annual growth factor] = 1 + μ. Contributions are made at the
 * end of each month and step up by {@code contributionGrowthRate} at the start of each year.
 */
public final class MonteCarloSimulator {

    /** Caps iterations × months to keep latency around one second. */
    public static final long MAX_MONTH_STEPS = 10_000_000L;
    static final int HISTOGRAM_BINS = 30;
    /** Generated seeds stay below 2^53 so JavaScript clients can echo them back without precision loss. */
    static final long MAX_GENERATED_SEED = 1L << 53;
    /** Thresholds are compared at cent precision: month-by-month sums drift by ~1e-10 from the exact amount. */
    private static final double HALF_CENT = 0.005;

    public MonteCarloResult simulate(MonteCarloParameters p) {
        int years = p.years();
        int n = p.iterations();
        long steps = (long) n * years * 12;
        if (steps > MAX_MONTH_STEPS) {
            throw new IllegalArgumentException(
                    "Combinação de simulações × meses muito grande (" + steps + " > " + MAX_MONTH_STEPS
                            + "). Reduza o número de simulações ou o horizonte.");
        }

        long seed = p.seed() != null ? p.seed() : new SplittableRandom().nextLong(MAX_GENERATED_SEED);
        SplittableRandom rng = new SplittableRandom(seed);

        double s = p.annualVolatility() / Math.sqrt(12);
        double m = Math.log1p(p.expectedAnnualReturn()) / 12 - s * s / 2;

        double[] monthlyContribution = new double[years];
        for (int y = 0; y < years; y++) {
            monthlyContribution[y] = p.monthlyContribution() * Math.pow(1 + p.contributionGrowthRate(), y);
        }

        // byYear[y][i] = value of path i at the end of year y (year 0 = initial amount)
        double[][] byYear = new double[years + 1][n];
        for (int i = 0; i < n; i++) {
            double v = p.initialAmount();
            byYear[0][i] = v;
            for (int y = 1; y <= years; y++) {
                double c = monthlyContribution[y - 1];
                for (int k = 0; k < 12; k++) {
                    v = v * Math.exp(m + s * rng.nextGaussian()) + c;
                }
                byYear[y][i] = v;
            }
        }

        double monthlyExpectedFactor = Math.pow(1 + p.expectedAnnualReturn(), 1.0 / 12);
        double expected = p.initialAmount();
        double contributed = p.initialAmount();
        List<YearBand> bands = new ArrayList<>(years + 1);
        for (int y = 0; y <= years; y++) {
            if (y > 0) {
                double c = monthlyContribution[y - 1];
                for (int k = 0; k < 12; k++) {
                    expected = expected * monthlyExpectedFactor + c;
                }
                contributed += 12 * c;
            }
            double[] values = byYear[y];
            Arrays.sort(values);
            bands.add(new YearBand(y, percentile(values, 0.05), percentile(values, 0.25), percentile(values, 0.50),
                    percentile(values, 0.75), percentile(values, 0.95), expected, contributed));
        }

        double[] finals = byYear[years];
        if (!Double.isFinite(finals[n - 1]) || !Double.isFinite(expected)) {
            throw new CalculationException("Valores simulados excederam o limite numérico. Reduza retorno, volatilidade ou horizonte.");
        }

        double sum = 0;
        for (double v : finals) sum += v;

        Double nominalTarget = null;
        Double probabilityOfReachingTarget = null;
        if (p.targetAmount() != null) {
            nominalTarget = p.annualInflation() != null
                    ? p.targetAmount() * Math.pow(1 + p.annualInflation(), years)
                    : p.targetAmount();
            probabilityOfReachingTarget = (double) (n - lowerBound(finals, nominalTarget - HALF_CENT)) / n;
        }
        double probabilityBelowContributed = (double) lowerBound(finals, contributed - HALF_CENT) / n;

        Percentiles finalPercentiles = new Percentiles(
                percentile(finals, 0.05), percentile(finals, 0.10), percentile(finals, 0.25), percentile(finals, 0.50),
                percentile(finals, 0.75), percentile(finals, 0.90), percentile(finals, 0.95));

        List<HistogramBin> histogram = histogram(finals);
        int histogramOverflowCount = n - histogram.stream().mapToInt(HistogramBin::count).sum();

        return new MonteCarloResult(contributed, expected, sum / n, finalPercentiles, bands,
                nominalTarget, probabilityOfReachingTarget, probabilityBelowContributed,
                histogram, histogramOverflowCount, n, seed);
    }

    /** Linear interpolation between closest ranks (Hyndman–Fan type 7) on a sorted array. */
    static double percentile(double[] sorted, double q) {
        double h = (sorted.length - 1) * q;
        int lo = (int) Math.floor(h);
        int hi = Math.min(lo + 1, sorted.length - 1);
        return sorted[lo] + (h - lo) * (sorted[hi] - sorted[lo]);
    }

    /** Number of elements strictly below {@code x} in a sorted array. */
    private static int lowerBound(double[] sorted, double x) {
        int lo = 0, hi = sorted.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (sorted[mid] < x) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    /**
     * Equal-width bins from the minimum to P99 so the long right tail does not flatten the chart;
     * values above P99 are left out of the bins.
     */
    private static List<HistogramBin> histogram(double[] sorted) {
        double lo = sorted[0];
        double hi = percentile(sorted, 0.99);
        if (hi <= lo) {
            return List.of(new HistogramBin(lo, sorted[sorted.length - 1], sorted.length));
        }
        double width = (hi - lo) / HISTOGRAM_BINS;
        int[] counts = new int[HISTOGRAM_BINS];
        for (double v : sorted) {
            if (v <= hi) {
                counts[Math.min((int) ((v - lo) / width), HISTOGRAM_BINS - 1)]++;
            }
        }
        List<HistogramBin> bins = new ArrayList<>(HISTOGRAM_BINS);
        for (int b = 0; b < HISTOGRAM_BINS; b++) {
            bins.add(new HistogramBin(lo + b * width, b == HISTOGRAM_BINS - 1 ? hi : lo + (b + 1) * width, counts[b]));
        }
        return bins;
    }
}
