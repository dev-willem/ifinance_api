package com.willembergfilho.ifinance.domain.montecarlo;

/**
 * Rates are decimals (0.10 = 10% a.a.). When {@code annualInflation} is given, {@code targetAmount}
 * is read in today's money and inflated to the horizon before comparing with nominal final values.
 */
public record MonteCarloParameters(
        double initialAmount,
        double monthlyContribution,
        double expectedAnnualReturn,
        double annualVolatility,
        int years,
        int iterations,
        Double targetAmount,
        Double annualInflation,
        double contributionGrowthRate,
        Long seed
) {
    public MonteCarloParameters {
        requireFinite(initialAmount, expectedAnnualReturn, annualVolatility, contributionGrowthRate, monthlyContribution);
        if (initialAmount < 0 || monthlyContribution < 0) {
            throw new IllegalArgumentException("Valor inicial e aporte mensal não podem ser negativos.");
        }
        if (initialAmount == 0 && monthlyContribution == 0) {
            throw new IllegalArgumentException("Informe um valor inicial ou um aporte mensal maior que zero.");
        }
        if (expectedAnnualReturn <= -1) {
            throw new IllegalArgumentException("Retorno anual esperado deve ser maior que -100%.");
        }
        if (annualVolatility < 0) {
            throw new IllegalArgumentException("Volatilidade anual não pode ser negativa.");
        }
        if (years < 1) {
            throw new IllegalArgumentException("Horizonte deve ser de pelo menos 1 ano.");
        }
        if (iterations < 1) {
            throw new IllegalArgumentException("Número de simulações deve ser positivo.");
        }
        if (targetAmount != null && !(targetAmount > 0 && Double.isFinite(targetAmount))) {
            throw new IllegalArgumentException("Meta deve ser maior que zero.");
        }
        if (annualInflation != null && !(annualInflation > -1 && Double.isFinite(annualInflation))) {
            throw new IllegalArgumentException("Inflação anual deve ser maior que -100%.");
        }
        if (contributionGrowthRate <= -1) {
            throw new IllegalArgumentException("Crescimento anual dos aportes deve ser maior que -100%.");
        }
    }

    private static void requireFinite(double... values) {
        for (double v : values) {
            if (!Double.isFinite(v)) {
                throw new IllegalArgumentException("Parâmetros numéricos devem ser finitos.");
            }
        }
    }
}
