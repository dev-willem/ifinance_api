package com.willembergfilho.ifinance.domain.labor;

import com.willembergfilho.ifinance.domain.labor.IrrfResult.DeductionMethod;
import com.willembergfilho.ifinance.domain.labor.LaborTaxTable.InssBracket;
import com.willembergfilho.ifinance.domain.labor.LaborTaxTable.IrrfBracket;
import com.willembergfilho.ifinance.domain.labor.LaborTaxTable.IrrfReduction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MODE;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.round;
import static java.math.BigDecimal.ZERO;

/** Employee INSS and IRRF withholding shared by every labor calculator. */
public final class PayrollTaxes {

    static final BigDecimal FGTS_RATE = new BigDecimal("0.08"); // Lei 8.036/1990, art. 15
    static final BigDecimal THIRTY = BigDecimal.valueOf(30);
    static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    static final BigDecimal THREE = BigDecimal.valueOf(3);
    private static final BigDecimal CENT = new BigDecimal("0.01");

    private PayrollTaxes() {}

    /** Progressive employee INSS; amounts above the ceiling are not taxed. */
    public static InssResult inss(BigDecimal contributionBase, LaborTaxTable table) {
        requireNonNegative(contributionBase, "Base de cálculo do INSS");
        List<InssResult.BracketShare> shares = new ArrayList<>();
        BigDecimal lower = ZERO;
        BigDecimal total = ZERO;
        for (InssBracket b : table.inssBrackets()) {
            if (contributionBase.compareTo(lower) <= 0) break;
            BigDecimal taxed = contributionBase.min(b.upTo()).subtract(lower);
            BigDecimal contribution = taxed.multiply(b.rate(), MC);
            shares.add(new InssResult.BracketShare(lower.signum() == 0 ? ZERO : lower.add(CENT), b.upTo(),
                    b.rate(), round(taxed), round(contribution)));
            total = total.add(contribution);
            lower = b.upTo();
        }
        BigDecimal rounded = round(total);
        return new InssResult(round(contributionBase), shares, rounded, ratio(rounded, contributionBase));
    }

    /**
     * Monthly IRRF using the more favorable of legal deductions and the simplified discount,
     * then the Lei 15.270/2025 reduction, which is computed over the gross taxable income
     * (Receita Federal orientation, Jan/2026) and capped at the tax due.
     */
    public static IrrfResult irrf(BigDecimal taxableIncome, BigDecimal inss, int dependents,
                                  BigDecimal alimony, LaborTaxTable table) {
        requireNonNegative(taxableIncome, "Rendimento tributável");
        BigDecimal dependentsDeduction = round(table.dependentDeduction().multiply(BigDecimal.valueOf(dependents)));
        BigDecimal legal = inss.add(dependentsDeduction).add(alimony);
        boolean simplified = table.simplifiedDiscount().compareTo(legal) > 0;
        BigDecimal applied = simplified ? table.simplifiedDiscount() : legal;

        BigDecimal base = round(taxableIncome.subtract(applied).max(ZERO));
        IrrfBracket bracket = table.irrfBrackets().stream()
                .filter(b -> b.upTo() == null || base.compareTo(b.upTo()) <= 0)
                .findFirst().orElseThrow();
        BigDecimal taxBefore = round(base.multiply(bracket.rate(), MC).subtract(bracket.deduction()).max(ZERO));
        BigDecimal reduction = reduction(taxableIncome, taxBefore, table.irrfReduction());
        BigDecimal tax = taxBefore.subtract(reduction);

        return new IrrfResult(round(taxableIncome), inss, dependentsDeduction, alimony, legal,
                table.simplifiedDiscount(), simplified ? DeductionMethod.SIMPLIFIED : DeductionMethod.LEGAL,
                applied, base, bracket.rate(), bracket.deduction(), taxBefore, reduction, tax,
                ratio(tax, taxableIncome));
    }

    private static BigDecimal reduction(BigDecimal income, BigDecimal tax, IrrfReduction r) {
        if (r == null || income.compareTo(r.phaseOutUpTo()) > 0) return ZERO;
        BigDecimal value = income.compareTo(r.fullUpTo()) <= 0
                ? r.maxReduction()
                : round(r.constant().subtract(r.slope().multiply(income, MC)).max(ZERO));
        return value.min(tax);
    }

    /** a / b with 4 decimals (0.1234 = 12.34%); zero when b is zero. */
    static BigDecimal ratio(BigDecimal a, BigDecimal b) {
        return b.signum() == 0 ? ZERO.setScale(4) : a.divide(b, 4, MODE);
    }

    /** amount x numerator / denominator, rounded to cents. */
    static BigDecimal fraction(BigDecimal amount, long numerator, BigDecimal denominator) {
        return round(amount.multiply(BigDecimal.valueOf(numerator)).divide(denominator, MC));
    }

    static String percentLabel(BigDecimal rate) {
        return rate.movePointRight(2).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros()
                .toPlainString().replace('.', ',') + "%";
    }

    static BigDecimal orZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    static void requireNonNegative(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(label + " deve ser maior ou igual a zero.");
        }
    }

    static void requirePositive(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(label + " deve ser maior que zero.");
        }
    }

    static void requireRange(int value, int min, int max, String label) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(label + " deve estar entre " + min + " e " + max + ".");
        }
    }
}
