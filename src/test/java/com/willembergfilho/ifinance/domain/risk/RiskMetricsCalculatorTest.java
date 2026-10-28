package com.willembergfilho.ifinance.domain.risk;

import com.willembergfilho.ifinance.domain.math.CalculationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RiskMetricsCalculatorTest {

    private static final double EPS = 1e-6;
    private final RiskMetricsCalculator calc = new RiskMetricsCalculator();

    // r = [10%, -5%, 2%, -3%]; mean = 0.04/4 = 0.01
    // deviations 0.09, -0.06, 0.01, -0.04 -> squares sum 0.0134 -> sample var 0.0134/3 -> stdev 0.0668331
    private static final List<Double> SERIES = List.of(0.10, -0.05, 0.02, -0.03);

    private static RiskMetricsParameters params(List<Double> values, RiskInputType type, List<Double> bench,
                                                Periodicity periodicity, double rf, double confidence) {
        return new RiskMetricsParameters(values, type, bench, periodicity, rf, confidence);
    }

    private RiskMetricsResult annual(List<Double> returns) {
        return calc.calculate(params(returns, RiskInputType.RETURNS, null, Periodicity.ANNUAL, 0, 0.95));
    }

    // --------------------------------------------------------------- returns --

    @Test
    void basicStatistics() {
        RiskMetricsResult r = annual(SERIES);

        assertThat(r.periods()).isEqualTo(4);
        assertThat(r.meanPeriodReturn()).isCloseTo(0.01, within(EPS));
        assertThat(r.periodVolatility()).isCloseTo(Math.sqrt(0.0134 / 3), within(1e-12));
        assertThat(r.periodVolatility()).isCloseTo(0.0668331, within(EPS));
        // wealth 1.1 * 0.95 * 1.02 * 0.97 = 1.033923
        assertThat(r.cumulativeReturn()).isCloseTo(0.033923, within(EPS));
        // CAGR with k=1 over 4 periods: 1.033923^(1/4) - 1
        assertThat(r.annualizedReturn()).isCloseTo(0.0083749, within(EPS));
    }

    @Test
    void annualizedVolatilityScalesBySqrtK() {
        RiskMetricsResult r = calc.calculate(params(SERIES, RiskInputType.RETURNS, null, Periodicity.MONTHLY, 0, 0.95));
        // 0.0668331 * sqrt(12) = 0.231517
        assertThat(r.annualizedVolatility()).isCloseTo(0.231517, within(EPS));
        // 1.033923^(12/4) - 1 = 1.0689968 * 1.033923 - 1 = 0.105260
        assertThat(r.annualizedReturn()).isCloseTo(0.105260, within(EPS));
    }

    @Test
    void sharpe_annualRiskFreeConvertedGeometrically() {
        double rfAnnual = Math.pow(1.005, 12) - 1;
        RiskMetricsResult r = calc.calculate(
                params(SERIES, RiskInputType.RETURNS, null, Periodicity.MONTHLY, rfAnnual, 0.95));
        assertThat(r.riskFreePeriodRate()).isCloseTo(0.005, within(1e-12));
        // (0.01 - 0.005) * 12 / 0.231517 = 0.259161
        assertThat(r.sharpeRatio()).isCloseTo(0.259161, within(EPS));
    }

    @Test
    void sortino_usesDownsideDeviationAgainstRiskFree() {
        RiskMetricsResult r = annual(SERIES);
        // rf 0: shortfalls -0.05, -0.03 -> (0.0025 + 0.0009)/4 = 0.00085 -> dd 0.0291548
        assertThat(r.downsideDeviationAnnualized()).isCloseTo(0.0291548, within(EPS));
        // 0.01 / 0.0291548 = 0.342997; Sharpe 0.01 / 0.0668331 = 0.149627
        assertThat(r.sortinoRatio()).isCloseTo(0.342997, within(EPS));
        assertThat(r.sharpeRatio()).isCloseTo(0.149627, within(EPS));
    }

    // -------------------------------------------------------------- drawdown --

    @Test
    void drawdown_withoutRecovery() {
        RiskMetricsResult r = annual(SERIES);
        // wealth 1, 1.1, 1.045, 1.0659, 1.033923 -> trough at end: 1.033923/1.1 - 1 = -0.060070
        assertThat(r.maxDrawdown()).isCloseTo(-0.060070, within(EPS));
        assertThat(r.drawdownPeakIndex()).isEqualTo(1);
        assertThat(r.drawdownTroughIndex()).isEqualTo(4);
        assertThat(r.drawdownRecoveryIndex()).isNull();
        // Calmar = 0.0083749 / 0.060070
        assertThat(r.calmarRatio()).isCloseTo(0.0083749 / 0.060070, within(1e-5));
        assertThat(r.wealthIndex()).hasSize(5);
        assertThat(r.drawdownSeries().get(2)).isCloseTo(-0.05, within(EPS));
    }

    @Test
    void drawdown_withRecovery() {
        // wealth 1, 1.1, 0.88, 1.1, 1.1 -> -20% from index 1 to 2, recovered at 3
        RiskMetricsResult r = annual(List.of(0.10, -0.20, 0.25, 0.0));
        assertThat(r.maxDrawdown()).isCloseTo(-0.20, within(EPS));
        assertThat(r.drawdownPeakIndex()).isEqualTo(1);
        assertThat(r.drawdownTroughIndex()).isEqualTo(2);
        assertThat(r.drawdownRecoveryIndex()).isEqualTo(3);
    }

    // ------------------------------------------------------------------- VaR --

    @Test
    void historicalVarAndCvar() {
        // sorted: -0.05, -0.03, 0.02, 0.10
        // c=95%: ceil(4*0.05)=1 worst -> VaR = CVaR = 5%
        RiskMetricsResult r95 = annual(SERIES);
        assertThat(r95.historicalVar()).isCloseTo(0.05, within(EPS));
        assertThat(r95.historicalCvar()).isCloseTo(0.05, within(EPS));
        // c=50%: 2 worst -> VaR = 3%, CVaR = (5% + 3%)/2 = 4%
        RiskMetricsResult r50 = calc.calculate(params(SERIES, RiskInputType.RETURNS, null, Periodicity.ANNUAL, 0, 0.5));
        assertThat(r50.historicalVar()).isCloseTo(0.03, within(EPS));
        assertThat(r50.historicalCvar()).isCloseTo(0.04, within(EPS));
    }

    @Test
    void var_tailCountNotInflatedByFloatingPoint() {
        // 20 * (1 - 0.95) is 1.0000000000000009 in double; tail must still be 1
        List<Double> returns = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) returns.add(i == 0 ? -0.10 : i == 1 ? -0.08 : 0.01);
        RiskMetricsResult r = annual(returns);
        assertThat(r.historicalVar()).isCloseTo(0.10, within(EPS));
        assertThat(r.historicalCvar()).isCloseTo(0.10, within(EPS));
    }

    // ---------------------------------------------------------------- prices --

    @Test
    void prices_convertedToSimpleReturns() {
        // 100 -> 110 -> 99 -> 99: +10%, -10%, 0%
        RiskMetricsResult r = calc.calculate(
                params(List.of(100.0, 110.0, 99.0, 99.0), RiskInputType.PRICES, null, Periodicity.ANNUAL, 0, 0.95));
        assertThat(r.periods()).isEqualTo(3);
        assertThat(r.meanPeriodReturn()).isCloseTo(0.0, within(EPS));
        assertThat(r.cumulativeReturn()).isCloseTo(-0.01, within(EPS));
        assertThat(RiskMetricsCalculator.toReturns(List.of(100.0, 110.0, 99.0), RiskInputType.PRICES, "x"))
                .containsExactly(new double[]{0.10, -0.10}, within(1e-12));
    }

    // ------------------------------------------------------------- benchmark --

    @Test
    void benchmark_assetEqualsBenchmark_betaOneAlphaZero() {
        RiskMetricsResult r = calc.calculate(
                params(SERIES, RiskInputType.RETURNS, SERIES, Periodicity.MONTHLY, 0.10, 0.95));
        RiskMetricsResult.BenchmarkMetrics b = r.benchmark();
        assertThat(b.beta()).isCloseTo(1.0, within(1e-12));
        assertThat(b.jensensAlphaAnnualized()).isCloseTo(0.0, within(1e-12));
        assertThat(b.correlation()).isCloseTo(1.0, within(1e-12));
        assertThat(b.rSquared()).isCloseTo(1.0, within(1e-12));
        assertThat(b.trackingErrorAnnualized()).isZero();
        assertThat(b.informationRatio()).isNull();
        assertThat(r.benchmarkWealthIndex()).isEqualTo(r.wealthIndex());
    }

    @Test
    void benchmark_leveredAsset() {
        // b = [1%, 2%, -1%, 0%], r = 2b + 0.1% -> beta 2, corr 1
        // mean b = 0.005, mean r = 0.011; alpha/period = 0.011 - 2*0.005 = 0.001 -> x12 = 0.012
        // active = b + 0.001 -> stdev = stdev(b) = sqrt(0.0005/3) = 0.0129099; mean active = 0.006
        List<Double> b = List.of(0.01, 0.02, -0.01, 0.0);
        List<Double> r = List.of(0.021, 0.041, -0.019, 0.001);
        RiskMetricsResult.BenchmarkMetrics m = calc.calculate(
                params(r, RiskInputType.RETURNS, b, Periodicity.MONTHLY, 0, 0.95)).benchmark();
        assertThat(m.beta()).isCloseTo(2.0, within(1e-9));
        assertThat(m.correlation()).isCloseTo(1.0, within(1e-9));
        assertThat(m.rSquared()).isCloseTo(1.0, within(1e-9));
        assertThat(m.jensensAlphaAnnualized()).isCloseTo(0.012, within(1e-9));
        // TE = 0.0129099 * sqrt(12) = 0.0447214; IR = 0.006*12 / 0.0447214 = 1.609969
        assertThat(m.trackingErrorAnnualized()).isCloseTo(0.0447214, within(EPS));
        assertThat(m.informationRatio()).isCloseTo(1.609969, within(EPS));
    }

    @Test
    void benchmark_zeroVariance_betaAndCorrelationNull() {
        RiskMetricsResult.BenchmarkMetrics m = calc.calculate(params(SERIES, RiskInputType.RETURNS,
                List.of(0.01, 0.01, 0.01, 0.01), Periodicity.ANNUAL, 0, 0.95)).benchmark();
        assertThat(m.beta()).isNull();
        assertThat(m.jensensAlphaAnnualized()).isNull();
        assertThat(m.correlation()).isNull();
        assertThat(m.rSquared()).isNull();
        assertThat(m.informationRatio()).isNotNull();
    }

    // ------------------------------------------------------------ edge cases --

    @Test
    void zeroVariance_ratiosAreNull() {
        RiskMetricsResult r = annual(List.of(0.01, 0.01, 0.01, 0.01, 0.01));
        assertThat(r.sharpeRatio()).isNull();
        assertThat(r.sortinoRatio()).isNull();
        assertThat(r.maxDrawdown()).isZero();
        assertThat(r.drawdownPeakIndex()).isNull();
        assertThat(r.drawdownTroughIndex()).isNull();
        assertThat(r.calmarRatio()).isNull();
    }

    @Test
    void totalLoss_isSupported() {
        RiskMetricsResult r = annual(List.of(0.10, -1.0));
        assertThat(r.cumulativeReturn()).isCloseTo(-1.0, within(EPS));
        assertThat(r.annualizedReturn()).isCloseTo(-1.0, within(EPS));
        assertThat(r.maxDrawdown()).isCloseTo(-1.0, within(EPS));
    }

    @Test
    void tooFewReturns_throwsCalculationException() {
        assertThatThrownBy(() -> calc.calculate(
                params(List.of(100.0, 101.0), RiskInputType.PRICES, null, Periodicity.DAILY, 0, 0.95)))
                .isInstanceOf(CalculationException.class)
                .hasMessageContaining("ao menos 2 retornos");
    }

    @Test
    void invalidInputs_throwIllegalArgument() {
        assertThatIllegalArgumentException().isThrownBy(() -> calc.calculate(
                params(List.of(100.0, 0.0, 10.0), RiskInputType.PRICES, null, Periodicity.DAILY, 0, 0.95)));
        assertThatIllegalArgumentException().isThrownBy(() -> annual(List.of(0.1, -1.5)));
        assertThatIllegalArgumentException().isThrownBy(() -> calc.calculate(
                params(SERIES, RiskInputType.RETURNS, List.of(0.1, 0.2), Periodicity.DAILY, 0, 0.95)))
                .withMessageContaining("mesmo número");
        assertThatIllegalArgumentException().isThrownBy(() -> calc.calculate(
                params(SERIES, RiskInputType.RETURNS, null, Periodicity.DAILY, 0, 1.0)));
    }

    @Test
    void extremeValues_throwCalculationException() {
        assertThatThrownBy(() -> calc.calculate(params(List.of(1e-300, 1e300, 1e300),
                RiskInputType.PRICES, null, Periodicity.DAILY, 0, 0.95)))
                .isInstanceOf(CalculationException.class);
    }

    @Test
    void varianceOverflow_throwsInsteadOfZeroSharpe() {
        // finite wealth (~5.5e199) but squared deviations of ~1e200 overflow the variance
        assertThatThrownBy(() -> annual(List.of(1e200, -0.5, 0.1)))
                .isInstanceOf(CalculationException.class);
        assertThatThrownBy(() -> calc.calculate(params(SERIES, RiskInputType.RETURNS,
                List.of(1e200, -0.5, 0.1, 0.0), Periodicity.DAILY, 0, 0.95)))
                .isInstanceOf(CalculationException.class)
                .hasMessageContaining("benchmark");
    }
}
