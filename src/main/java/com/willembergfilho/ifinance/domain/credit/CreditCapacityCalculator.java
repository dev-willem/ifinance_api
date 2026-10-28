package com.willembergfilho.ifinance.domain.credit;

import com.willembergfilho.ifinance.domain.math.MonetaryRounding;
import com.willembergfilho.ifinance.domain.math.RateConverter;
import com.willembergfilho.ifinance.domain.simulation.AmortizationSystem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;

public class CreditCapacityCalculator {

    public static final int MAX_TERM_MONTHS = 480;
    public static final BigDecimal DEFAULT_COMMITMENT_RATE = new BigDecimal("0.30");
    private static final BigDecimal MIN_COMMITMENT_RATE = new BigDecimal("0.05");
    private static final BigDecimal MAX_COMMITMENT_RATE = new BigDecimal("0.70");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final RateConverter rateConverter = new RateConverter();

    public CreditCapacityResult calculate(CreditCapacityParameters p) {
        validate(p);
        BigDecimal commitmentRate = p.maxCommitmentRate() != null ? p.maxCommitmentRate() : DEFAULT_COMMITMENT_RATE;
        BigDecimal monthlyRate = rateConverter.toPeriodicRate(p.annualInterestRate(), 12);
        int n = p.termMonths();

        BigDecimal maxInstallment = MonetaryRounding.round(
                p.monthlyIncome().multiply(commitmentRate, MC).subtract(p.existingMonthlyDebts(), MC).max(BigDecimal.ZERO));
        BigDecimal currentCommitment = percentOfIncome(p.existingMonthlyDebts(), p.monthlyIncome());
        BigDecimal maxLoan = MonetaryRounding.round(
                maxInstallment.divide(firstInstallmentFactor(p.amortizationSystem(), monthlyRate, n), MC));

        BigDecimal desired = p.desiredLoanAmount();
        BigDecimal desiredFirst = null;
        BigDecimal resultingCommitment = null;
        Boolean approved = null;
        Integer minimumTerm = null;
        if (desired != null) {
            desiredFirst = firstInstallment(desired, p.amortizationSystem(), monthlyRate, n);
            resultingCommitment = percentOfIncome(p.existingMonthlyDebts().add(desiredFirst, MC), p.monthlyIncome());
            // without margin, an installment that rounds to 0.00 must not count as fitting
            boolean hasMargin = maxInstallment.signum() > 0;
            approved = hasMargin && desiredFirst.compareTo(maxInstallment) <= 0;
            minimumTerm = hasMargin ? minimumTerm(desired, p.amortizationSystem(), monthlyRate, maxInstallment) : null;
        }

        return new CreditCapacityResult(
                p.amortizationSystem(),
                commitmentRate,
                monthlyRate,
                maxInstallment,
                currentCommitment,
                maxLoan,
                desired,
                desiredFirst,
                resultingCommitment,
                approved,
                minimumTerm,
                statusMessage(commitmentRate, maxInstallment, maxLoan, n, approved, minimumTerm,
                        p.existingMonthlyDebts().signum() > 0)
        );
    }

    /** First installment = principal x factor. PRICE: i/(1-(1+i)^-n); SAC: 1/n + i (first is the highest). */
    static BigDecimal firstInstallmentFactor(AmortizationSystem system, BigDecimal i, int n) {
        BigDecimal nBd = BigDecimal.valueOf(n);
        if (system == AmortizationSystem.SAC) {
            return BigDecimal.ONE.divide(nBd, MC).add(i, MC);
        }
        if (i.signum() == 0) {
            return BigDecimal.ONE.divide(nBd, MC);
        }
        BigDecimal discount = BigDecimal.ONE.divide(BigDecimal.ONE.add(i, MC).pow(n, MC), MC);
        return i.divide(BigDecimal.ONE.subtract(discount, MC), MC);
    }

    private static BigDecimal firstInstallment(BigDecimal principal, AmortizationSystem system, BigDecimal i, int n) {
        return MonetaryRounding.round(principal.multiply(firstInstallmentFactor(system, i, n), MC));
    }

    /** Installment decreases with the term in both systems, so the first fitting term is the minimum. */
    private static Integer minimumTerm(BigDecimal principal, AmortizationSystem system, BigDecimal i, BigDecimal limit) {
        if (firstInstallment(principal, system, i, MAX_TERM_MONTHS).compareTo(limit) > 0) {
            return null;
        }
        int lo = 1, hi = MAX_TERM_MONTHS;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (firstInstallment(principal, system, i, mid).compareTo(limit) <= 0) hi = mid;
            else lo = mid + 1;
        }
        return lo;
    }

    private static BigDecimal percentOfIncome(BigDecimal amount, BigDecimal income) {
        return amount.multiply(HUNDRED, MC).divide(income, 2, RoundingMode.HALF_EVEN);
    }

    private static String statusMessage(BigDecimal commitmentRate, BigDecimal maxInstallment, BigDecimal maxLoan,
                                        int n, Boolean approved, Integer minimumTerm, boolean hasDebts) {
        NumberFormat percent = NumberFormat.getNumberInstance(PT_BR);
        percent.setMaximumFractionDigits(4);
        String limit = percent.format(commitmentRate.multiply(HUNDRED)) + "%";
        if (maxInstallment.signum() == 0) {
            return (hasDebts
                    ? "Suas dívidas atuais já consomem o limite de " + limit + " da renda."
                    : "O limite de " + limit + " da renda é inferior a R$ 0,01.")
                    + " Não há margem para novas parcelas.";
        }
        if (approved == null) {
            return "Com o limite de " + limit + " da renda, sua parcela máxima é de " + brl(maxInstallment)
                    + ", o que permite financiar até " + brl(maxLoan) + " em " + n + " meses.";
        }
        if (approved) {
            return "Aprovado: a primeira parcela do valor desejado cabe no limite de " + limit + " da renda em " + n + " meses.";
        }
        if (minimumTerm != null) {
            return "Não aprovado em " + n + " meses: a parcela ultrapassa o limite de " + limit
                    + " da renda. O prazo mínimo que cabe no orçamento é de " + minimumTerm + " meses.";
        }
        return "Não aprovado: o valor desejado não cabe no limite de " + limit + " da renda em nenhum prazo de até "
                + MAX_TERM_MONTHS + " meses.";
    }

    private static String brl(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }

    private static void validate(CreditCapacityParameters p) {
        if (p.monthlyIncome() == null || p.monthlyIncome().signum() <= 0) {
            throw new IllegalArgumentException("A renda mensal deve ser maior que zero.");
        }
        if (p.existingMonthlyDebts() == null || p.existingMonthlyDebts().signum() < 0) {
            throw new IllegalArgumentException("As dívidas mensais atuais não podem ser negativas.");
        }
        if (p.maxCommitmentRate() != null && (p.maxCommitmentRate().compareTo(MIN_COMMITMENT_RATE) < 0
                || p.maxCommitmentRate().compareTo(MAX_COMMITMENT_RATE) > 0)) {
            throw new IllegalArgumentException("O comprometimento máximo deve estar entre 5% e 70% da renda.");
        }
        if (p.annualInterestRate() == null || p.annualInterestRate().signum() < 0) {
            throw new IllegalArgumentException("A taxa de juros anual não pode ser negativa.");
        }
        if (p.termMonths() < 1 || p.termMonths() > MAX_TERM_MONTHS) {
            throw new IllegalArgumentException("O prazo deve estar entre 1 e " + MAX_TERM_MONTHS + " meses.");
        }
        if (p.amortizationSystem() != AmortizationSystem.PRICE && p.amortizationSystem() != AmortizationSystem.SAC) {
            throw new IllegalArgumentException("Sistema de amortização não suportado. Use PRICE ou SAC.");
        }
        if (p.desiredLoanAmount() != null && p.desiredLoanAmount().signum() <= 0) {
            throw new IllegalArgumentException("O valor desejado deve ser maior que zero.");
        }
    }
}
