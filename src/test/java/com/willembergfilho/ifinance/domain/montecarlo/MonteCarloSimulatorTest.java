package com.willembergfilho.ifinance.domain.montecarlo;

import com.willembergfilho.ifinance.domain.montecarlo.MonteCarloResult.Percentiles;
import com.willembergfilho.ifinance.domain.montecarlo.MonteCarloResult.YearBand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MonteCarloSimulatorTest {

    private final MonteCarloSimulator simulator = new MonteCarloSimulator();

    private static MonteCarloParameters params(double initial, double monthly, double mu, double sigma,
                                               int years, int iterations, Double target, Double inflation,
                                               double growth, Long seed) {
        return new MonteCarloParameters(initial, monthly, mu, sigma, years, iterations, target, inflation, growth, seed);
    }

    @Test
    void sameSeed_producesIdenticalResults() {
        MonteCarloParameters p = params(10_000, 500, 0.10, 0.20, 10, 1000, 100_000.0, null, 0, 42L);

        assertThat(simulator.simulate(p)).isEqualTo(simulator.simulate(p));
    }

    @Test
    void differentSeeds_produceDifferentResults() {
        MonteCarloResult a = simulator.simulate(params(10_000, 500, 0.10, 0.20, 10, 1000, null, null, 0, 1L));
        MonteCarloResult b = simulator.simulate(params(10_000, 500, 0.10, 0.20, 10, 1000, null, null, 0, 2L));

        assertThat(a.finalPercentiles().p50()).isNotEqualTo(b.finalPercentiles().p50());
    }

    @Test
    void missingSeed_reportsGeneratedSeedThatReproducesRun() {
        MonteCarloResult first = simulator.simulate(params(1000, 100, 0.08, 0.15, 5, 200, null, null, 0, null));
        MonteCarloResult replay = simulator.simulate(params(1000, 100, 0.08, 0.15, 5, 200, null, null, 0, first.seed()));

        assertThat(replay).isEqualTo(first);
        assertThat(first.seed()).isBetween(0L, MonteCarloSimulator.MAX_GENERATED_SEED - 1);
    }

    @Test
    void zeroVolatility_matchesClosedFormFutureValue() {
        // r = 1.12^(1/12) - 1 ≈ 0.0094888, n = 120
        // FV = 10000·1.12^10 + 500·((1+r)^120 - 1)/r ≈ 31058.48 + 500·(2.1058482/0.0094888) ≈ 142023.6
        MonteCarloResult r = simulator.simulate(params(10_000, 500, 0.12, 0, 10, 100, null, null, 0, 7L));

        double rate = Math.pow(1.12, 1.0 / 12) - 1;
        double fv = 10_000 * Math.pow(1.12, 10) + 500 * (Math.pow(1 + rate, 120) - 1) / rate;
        assertThat(fv).isCloseTo(142_023.6, within(1.0));
        assertThat(r.finalPercentiles().p5()).isCloseTo(fv, within(0.01));
        assertThat(r.finalPercentiles().p95()).isCloseTo(fv, within(0.01));
        assertThat(r.mean()).isCloseTo(fv, within(0.01));
        assertThat(r.expectedFinalValue()).isCloseTo(fv, within(0.01));
        assertThat(r.totalContributed()).isEqualTo(70_000.0); // 10000 + 500·120
        assertThat(r.histogram()).hasSize(1);
        assertThat(r.histogramOverflowCount()).isZero();
        assertThat(r.probabilityBelowContributed()).isZero();
    }

    @Test
    void zeroReturnAndVolatility_finalEqualsContributions() {
        // 1000 + 100·12·2 = 3400
        MonteCarloResult r = simulator.simulate(params(1000, 100, 0, 0, 2, 100, null, null, 0, 1L));

        assertThat(r.finalPercentiles().p50()).isCloseTo(3400, within(1e-6));
        assertThat(r.totalContributed()).isEqualTo(3400.0);
    }

    @Test
    void contributionGrowth_stepsUpEachYear() {
        // zero return: 100·12 + 110·12 + 121·12 = 3972
        MonteCarloResult r = simulator.simulate(params(0, 100, 0, 0, 3, 100, null, null, 0.10, 1L));

        assertThat(r.totalContributed()).isCloseTo(3972, within(1e-6));
        assertThat(r.finalPercentiles().p50()).isCloseTo(3972, within(1e-6));
    }

    @Test
    void noContributions_medianCloseToAnalyticLognormalMedian() {
        // ln(V_T/V_0) ~ N(T·ln(1+μ) − σ²T/2, σ²T) → median = V_0·(1+μ)^T·e^(−σ²T/2)
        // = 100000·2.158925·e^(−0.1125) ≈ 100000·2.158925·0.893597 ≈ 192921
        MonteCarloResult r = simulator.simulate(params(100_000, 0, 0.08, 0.15, 10, 20_000, null, null, 0, 123L));

        double analyticMedian = 100_000 * Math.pow(1.08, 10) * Math.exp(-0.15 * 0.15 * 10 / 2);
        assertThat(analyticMedian).isCloseTo(192_921, within(10.0));
        assertThat(r.finalPercentiles().p50()).isCloseTo(analyticMedian, withinPercentage(2));
        // E[V_T] = V_0·(1+μ)^T
        assertThat(r.mean()).isCloseTo(100_000 * Math.pow(1.08, 10), withinPercentage(2));
    }

    @Test
    void percentilesAreMonotonicAndProbabilitiesAreBounded() {
        MonteCarloResult r = simulator.simulate(params(5000, 300, 0.07, 0.35, 20, 3000, 200_000.0, 0.04, 0.03, 99L));

        Percentiles p = r.finalPercentiles();
        assertThat(new double[]{p.p5(), p.p10(), p.p25(), p.p50(), p.p75(), p.p90(), p.p95()}).isSorted();
        for (YearBand b : r.yearlyBands()) {
            assertThat(new double[]{b.p5(), b.p25(), b.p50(), b.p75(), b.p95()}).isSorted();
        }
        assertThat(r.yearlyBands()).hasSize(21);
        assertThat(r.yearlyBands().getFirst().p50()).isEqualTo(5000.0);
        assertThat(r.probabilityOfReachingTarget()).isBetween(0.0, 1.0);
        assertThat(r.probabilityBelowContributed()).isBetween(0.0, 1.0);
        assertThat(r.histogram()).hasSize(MonteCarloSimulator.HISTOGRAM_BINS);
        int binned = r.histogram().stream().mapToInt(MonteCarloResult.HistogramBin::count).sum();
        assertThat(binned + r.histogramOverflowCount()).isEqualTo(3000);
        assertThat(r.histogramOverflowCount()).isLessThanOrEqualTo(30); // ~1% above P99
    }

    @Test
    void inflation_inflatesTargetToHorizon() {
        // 100000 in today's money at 5% a.a. for 10 years = 100000·1.05^10 ≈ 162889.46
        MonteCarloResult r = simulator.simulate(params(50_000, 1000, 0.10, 0.15, 10, 500, 100_000.0, 0.05, 0, 3L));

        assertThat(r.nominalTarget()).isCloseTo(162_889.46, within(0.01));
    }

    @Test
    void target_withoutInflation_isUsedAsIs_andZeroVolIsCertain() {
        // zero return, 1 year: 1000 + 100·12 = 2200
        MonteCarloResult hit = simulator.simulate(params(1000, 100, 0, 0, 1, 100, 2200.0, null, 0, 1L));
        MonteCarloResult miss = simulator.simulate(params(1000, 100, 0, 0, 1, 100, 2200.01, null, 0, 1L));

        assertThat(hit.nominalTarget()).isEqualTo(2200.0);
        assertThat(hit.probabilityOfReachingTarget()).isEqualTo(1.0);
        assertThat(miss.probabilityOfReachingTarget()).isEqualTo(0.0);
        assertThat(simulator.simulate(params(1000, 100, 0.1, 0.2, 1, 100, null, null, 0, 1L))
                .probabilityOfReachingTarget()).isNull();
    }

    @Test
    void cashScenario_finalEqualToContributed_isNotBelowAndReachesExactTarget() {
        // 1234.56·12·10 = 148147.20; the monthly sum lands at 148147.1999999998 in binary floating point
        MonteCarloResult r = simulator.simulate(params(0, 1234.56, 0, 0, 10, 100, 148_147.20, null, 0, 1L));
        assertThat(r.probabilityBelowContributed()).isEqualTo(0.0);
        assertThat(r.probabilityOfReachingTarget()).isEqualTo(1.0);

        // 1000 + 333.33·12 = 4999.96
        MonteCarloResult oneYear = simulator.simulate(params(1000, 333.33, 0, 0, 1, 100, 4999.96, null, 0, 1L));
        assertThat(oneYear.probabilityBelowContributed()).isEqualTo(0.0);
        assertThat(oneYear.probabilityOfReachingTarget()).isEqualTo(1.0);
    }

    @Test
    void percentile_interpolatesLinearly() {
        double[] sorted = {10, 20, 30, 40, 50};
        assertThat(MonteCarloSimulator.percentile(sorted, 0.5)).isEqualTo(30.0);
        assertThat(MonteCarloSimulator.percentile(sorted, 0.1)).isCloseTo(14.0, within(1e-9)); // h = 0.4
        assertThat(MonteCarloSimulator.percentile(sorted, 1.0)).isEqualTo(50.0);
    }

    @Test
    void rejectsWorkAboveCap() {
        assertThatThrownBy(() -> simulator.simulate(params(1000, 100, 0.1, 0.2, 60, 20_000, null, null, 0, 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reduza");
    }

    @Test
    void invalidParameters_throw() {
        assertThatThrownBy(() -> params(-1, 0, 0.1, 0.2, 1, 100, null, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> params(0, 0, 0.1, 0.2, 1, 100, null, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> params(100, 0, -1, 0.2, 1, 100, null, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> params(100, 0, 0.1, Double.NaN, 1, 100, null, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> params(100, 0, 0.1, 0.2, 0, 100, null, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> params(100, 0, 0.1, 0.2, 1, 100, 0.0, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void largestAllowedRun_finishesQuickly() {
        // 20000 iterations × 41 years × 12 = 9.84M monthly steps, just under the cap, with extreme μ/σ
        long start = System.nanoTime();
        MonteCarloResult r = simulator.simulate(params(1_000_000, 10_000, 1.0, 1.5, 41, 20_000, 1e9, 0.05, 0.1, 5L));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(r.iterations()).isEqualTo(20_000);
        assertThat(Double.isFinite(r.mean())).isTrue();
        assertThat(elapsedMs).isLessThan(5_000);
    }
}
