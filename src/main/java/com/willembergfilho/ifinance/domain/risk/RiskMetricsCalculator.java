package com.willembergfilho.ifinance.domain.risk;

import com.willembergfilho.ifinance.domain.math.CalculationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Ex-post risk metrics from a periodic series. Conventions:
 * arithmetic annualization of excess means (x k) and of deviations (x sqrt(k)) for Sharpe, Sortino,
 * alpha and information ratio; geometric (CAGR) for the annualized return; sample (n-1) moments;
 * downside deviation over n against the per-period risk-free rate; historical VaR/CVaR as positive
 * losses from the ceil(n(1-c)) worst returns.
 */
public class RiskMetricsCalculator {

    // Below this a standard deviation is floating-point noise, e.g. stdev of five equal returns.
    private static final double ZERO_TOLERANCE = 1e-12;

    public RiskMetricsResult calculate(RiskMetricsParameters p) {
        if (p.inputType() == null || p.periodicity() == null) {
            throw new IllegalArgumentException("Tipo de entrada e periodicidade são obrigatórios.");
        }
        if (!(p.confidenceLevel() > 0 && p.confidenceLevel() < 1)) {
            throw new IllegalArgumentException("O nível de confiança deve estar entre 0 e 1 (exclusive).");
        }
        if (!(p.riskFreeAnnualRate() > -1) || !Double.isFinite(p.riskFreeAnnualRate())) {
            throw new IllegalArgumentException("A taxa livre de risco anual deve ser maior que -100%.");
        }

        double[] r = toReturns(p.values(), p.inputType(), "série do ativo");
        double[] b = p.benchmarkValues() == null || p.benchmarkValues().isEmpty()
                ? null : toReturns(p.benchmarkValues(), p.inputType(), "série do benchmark");
        if (b != null && b.length != r.length) {
            throw new IllegalArgumentException(
                    "A série do benchmark deve ter o mesmo número de valores da série do ativo.");
        }

        int n = r.length;
        int k = p.periodicity().periodsPerYear();
        double rf = Math.pow(1 + p.riskFreeAnnualRate(), 1.0 / k) - 1;

        double[] wealth = wealthIndex(r);
        double cumulative = wealth[n] - 1;
        if (!Double.isFinite(cumulative)) {
            throw new CalculationException("Os valores informados geram retornos extremos demais para o cálculo.");
        }
        double annualized = Math.pow(wealth[n], (double) k / n) - 1;

        double mean = mean(r);
        double vol = stdev(r);
        if (!Double.isFinite(mean) || !Double.isFinite(vol)) {
            throw new CalculationException("Os valores informados geram retornos extremos demais para o cálculo.");
        }
        double annualVol = vol * Math.sqrt(k);
        Double sharpe = vol > ZERO_TOLERANCE ? (mean - rf) * k / annualVol : null;

        double downsideSq = 0;
        for (double x : r) {
            double d = Math.min(0, x - rf);
            downsideSq += d * d;
        }
        double downside = Math.sqrt(downsideSq / n);
        Double sortino = downside > ZERO_TOLERANCE ? (mean - rf) * k / (downside * Math.sqrt(k)) : null;

        double[] dd = new double[n + 1];
        int peak = 0, runningPeak = 0, trough = 0;
        for (int i = 0; i <= n; i++) {
            if (wealth[i] > wealth[runningPeak]) runningPeak = i;
            dd[i] = wealth[i] / wealth[runningPeak] - 1;
            if (dd[i] < dd[trough]) {
                trough = i;
                peak = runningPeak;
            }
        }
        double maxDrawdown = dd[trough];
        boolean hasDrawdown = maxDrawdown < 0;
        Integer recovery = null;
        if (hasDrawdown) {
            for (int j = trough + 1; j <= n; j++) {
                if (wealth[j] >= wealth[peak] * (1 - ZERO_TOLERANCE)) {
                    recovery = j;
                    break;
                }
            }
        }
        Double calmar = hasDrawdown ? annualized / -maxDrawdown : null;

        double[] sorted = r.clone();
        Arrays.sort(sorted);
        int tail = Math.max(1, (int) Math.ceil(n * (1 - p.confidenceLevel()) - 1e-9));
        double var = -sorted[tail - 1];
        double cvar = -mean(Arrays.copyOf(sorted, tail));

        RiskMetricsResult.BenchmarkMetrics bench = null;
        double[] benchWealth = null;
        if (b != null) {
            benchWealth = wealthIndex(b);
            bench = benchmarkMetrics(r, b, benchWealth, rf, k);
        }

        return new RiskMetricsResult(
                n, k, cumulative, annualized, mean, vol, annualVol, rf,
                sharpe, downside * Math.sqrt(k), sortino,
                maxDrawdown,
                hasDrawdown ? peak : null,
                hasDrawdown ? trough : null,
                recovery,
                calmar,
                p.confidenceLevel(), var, cvar,
                bench,
                toList(wealth), toList(dd), benchWealth == null ? null : toList(benchWealth)
        );
    }

    private RiskMetricsResult.BenchmarkMetrics benchmarkMetrics(double[] r, double[] b, double[] benchWealth,
                                                                double rf, int k) {
        int n = b.length;
        if (!Double.isFinite(benchWealth[n])) {
            throw new CalculationException("Os valores do benchmark geram retornos extremos demais para o cálculo.");
        }
        double meanR = mean(r);
        double meanB = mean(b);
        double varB = covariance(b, b);
        double sdR = stdev(r);
        double sdB = Math.sqrt(varB);
        double cov = covariance(r, b);
        double[] active = new double[n];
        for (int i = 0; i < n; i++) active[i] = r[i] - b[i];
        double te = stdev(active) * Math.sqrt(k);
        if (!Double.isFinite(varB) || !Double.isFinite(cov) || !Double.isFinite(te)) {
            throw new CalculationException("Os valores do benchmark geram retornos extremos demais para o cálculo.");
        }

        Double beta = sdB > ZERO_TOLERANCE ? cov / varB : null;
        Double alpha = beta != null ? ((meanR - rf) - beta * (meanB - rf)) * k : null;
        Double corr = sdB > ZERO_TOLERANCE && sdR > ZERO_TOLERANCE ? cov / (sdR * sdB) : null;

        Double ir = te > ZERO_TOLERANCE ? mean(active) * k / te : null;

        return new RiskMetricsResult.BenchmarkMetrics(
                benchWealth[n] - 1,
                Math.pow(benchWealth[n], (double) k / n) - 1,
                sdB * Math.sqrt(k),
                beta, alpha, corr,
                corr == null ? null : corr * corr,
                te, ir
        );
    }

    static double[] toReturns(List<Double> values, RiskInputType type, String label) {
        if (values == null || values.size() < 2) {
            throw new IllegalArgumentException("Informe ao menos 2 valores na " + label + ".");
        }
        for (Double v : values) {
            if (v == null || !Double.isFinite(v)) {
                throw new IllegalArgumentException("A " + label + " contém valores vazios ou inválidos.");
            }
        }
        double[] r;
        if (type == RiskInputType.PRICES) {
            for (Double v : values) {
                if (v <= 0) throw new IllegalArgumentException("Os preços da " + label + " devem ser positivos.");
            }
            r = new double[values.size() - 1];
            for (int i = 1; i < values.size(); i++) r[i - 1] = values.get(i) / values.get(i - 1) - 1;
        } else {
            r = new double[values.size()];
            for (int i = 0; i < r.length; i++) {
                r[i] = values.get(i);
                if (r[i] < -1) {
                    throw new IllegalArgumentException(
                            "Retornos da " + label + " não podem ser menores que -100% (-1).");
                }
            }
        }
        if (r.length < 2) {
            throw new CalculationException(
                    "São necessários ao menos 2 retornos (3 preços) na " + label + " para calcular as métricas.");
        }
        for (double x : r) {
            if (!Double.isFinite(x)) {
                throw new CalculationException("A " + label + " gera retornos extremos demais para o cálculo.");
            }
        }
        return r;
    }

    private static double[] wealthIndex(double[] r) {
        double[] w = new double[r.length + 1];
        w[0] = 1;
        for (int i = 0; i < r.length; i++) w[i + 1] = w[i] * (1 + r[i]);
        return w;
    }

    private static double mean(double[] x) {
        double s = 0;
        for (double v : x) s += v;
        return s / x.length;
    }

    private static double covariance(double[] x, double[] y) {
        double mx = mean(x), my = mean(y), s = 0;
        for (int i = 0; i < x.length; i++) s += (x[i] - mx) * (y[i] - my);
        return s / (x.length - 1);
    }

    private static double stdev(double[] x) {
        return Math.sqrt(covariance(x, x));
    }

    private static List<Double> toList(double[] x) {
        List<Double> out = new ArrayList<>(x.length);
        for (double v : x) out.add(v);
        return out;
    }
}
