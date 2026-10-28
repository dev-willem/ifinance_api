package com.willembergfilho.ifinance.domain.depreciation;

import com.willembergfilho.ifinance.domain.math.MonetaryRounding;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds fiscal-year depreciation schedules. Time-based methods are computed per "asset period"
 * (one nominal year of useful life) and then spread month by month over fiscal years, which handles
 * both the first-year pro-rata and the shift acceleration with the same mechanism.
 */
public final class DepreciationCalculator {

    private static final MathContext MC = MonetaryRounding.MC;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal MAX_DECLINING_FACTOR = new BigDecimal("4");
    // Bounds keep exact BigDecimal arithmetic (setScale, add) cheap: 1E+30000000 or 1E-30000000 would cost minutes.
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");
    private static final BigDecimal MAX_UNITS = new BigDecimal("1000000000000000");
    private static final int MAX_INPUT_SCALE = 10;

    // RIR/2018 (Decreto nº 9.580/2018), art. 323 (bens móveis): 1 turno de 8h = 1,0; 2 turnos = 1,5; 3 turnos = 2,0
    private static final BigDecimal[] SHIFT_COEFFICIENTS = {
            null, BigDecimal.ONE, new BigDecimal("1.5"), new BigDecimal("2.0")};
    // A nominal year of life lasts 12 / coefficient months: 12, 8 or 6.
    private static final int[] PERIOD_MONTHS = {0, 12, 8, 6};

    public static BigDecimal shiftCoefficient(int shiftsPerDay) {
        if (shiftsPerDay < 1 || shiftsPerDay > 3) {
            throw new IllegalArgumentException("O número de turnos diários deve estar entre 1 e 3.");
        }
        return SHIFT_COEFFICIENTS[shiftsPerDay];
    }

    public DepreciationSchedule calculate(DepreciationParameters p) {
        validate(p);
        BigDecimal asset = MonetaryRounding.round(p.assetValue());
        BigDecimal residual = MonetaryRounding.round(p.residualValue());
        if (residual.compareTo(asset) >= 0) {
            throw new IllegalArgumentException(
                    "O valor do ativo e o valor residual devem ter no mínimo R$ 0,01 de diferença.");
        }
        BigDecimal base = asset.subtract(residual);
        return p.method() == DepreciationMethod.UNITS_OF_PRODUCTION
                ? unitsOfProduction(p, asset, residual, base)
                : timeBased(p, asset, residual, base);
    }

    private DepreciationSchedule timeBased(DepreciationParameters p, BigDecimal asset,
                                           BigDecimal residual, BigDecimal base) {
        int life = p.usefulLifeYears();
        BigDecimal coefficient = shiftCoefficient(p.shiftsPerDay());
        int periodMonths = PERIOD_MONTHS[p.shiftsPerDay()];
        BigDecimal lifeBd = BigDecimal.valueOf(life);

        BigDecimal[] periodCharges = new BigDecimal[life];
        BigDecimal rate;
        switch (p.method()) {
            case STRAIGHT_LINE -> {
                BigDecimal charge = base.divide(lifeBd, MC);
                Arrays.fill(periodCharges, charge);
                rate = coefficient.divide(lifeBd, MC);
            }
            case SUM_OF_YEARS_DIGITS -> {
                BigDecimal digits = BigDecimal.valueOf((long) life * (life + 1) / 2);
                for (int i = 0; i < life; i++) {
                    periodCharges[i] = base.multiply(BigDecimal.valueOf(life - i), MC).divide(digits, MC);
                }
                rate = coefficient.multiply(lifeBd, MC).divide(digits, MC);
            }
            case DECLINING_BALANCE -> {
                BigDecimal periodRate = p.decliningFactor().divide(lifeBd, MC);
                BigDecimal book = asset;
                for (int i = 0; i < life; i++) {
                    BigDecimal remainingBase = book.subtract(residual, MC);
                    BigDecimal declining = book.multiply(periodRate, MC);
                    BigDecimal straight = remainingBase.divide(BigDecimal.valueOf(life - i), MC);
                    // Switch to straight-line once it yields the higher charge; never go below residual.
                    BigDecimal charge = declining.max(straight).min(remainingBase);
                    periodCharges[i] = charge;
                    book = book.subtract(charge, MC);
                }
                rate = coefficient.multiply(periodRate, MC);
            }
            default -> throw new IllegalStateException("Método não suportado: " + p.method());
        }

        int totalMonths = life * periodMonths;
        List<Integer> yearMonths = new ArrayList<>();
        List<BigDecimal> exactCharges = new ArrayList<>();
        BigDecimal periodMonthsBd = BigDecimal.valueOf(periodMonths);
        int month = 0;
        while (month < totalMonths) {
            int span = Math.min(yearMonths.isEmpty() ? p.firstYearMonths() : 12, totalMonths - month);
            BigDecimal sum = BigDecimal.ZERO;
            for (int m = month; m < month + span; m++) {
                sum = sum.add(periodCharges[m / periodMonths].divide(periodMonthsBd, MC), MC);
            }
            yearMonths.add(span);
            exactCharges.add(sum);
            month += span;
        }

        List<DepreciationRow> rows = buildRows(asset, base, yearMonths, exactCharges);
        BigDecimal effectiveLife = BigDecimal.valueOf(totalMonths)
                .divide(TWELVE, 2, MonetaryRounding.MODE);
        return schedule(p.method(), rate.multiply(HUNDRED, MC), effectiveLife, base, residual, rows);
    }

    private DepreciationSchedule unitsOfProduction(DepreciationParameters p, BigDecimal asset,
                                                   BigDecimal residual, BigDecimal base) {
        BigDecimal total = p.totalUnits();
        List<Integer> yearMonths = new ArrayList<>();
        List<BigDecimal> exactCharges = new ArrayList<>();
        BigDecimal used = BigDecimal.ZERO;
        for (BigDecimal units : p.unitsPerYear()) {
            if (used.compareTo(total) >= 0) break;
            BigDecimal yearUnits = units.min(total.subtract(used));
            used = used.add(yearUnits);
            yearMonths.add(12);
            exactCharges.add(base.multiply(yearUnits, MC).divide(total, MC));
        }
        BigDecimal firstYearUnits = p.unitsPerYear().getFirst().min(total);
        BigDecimal rate = firstYearUnits.multiply(HUNDRED, MC).divide(total, MC);
        List<DepreciationRow> rows = buildRows(asset, base, yearMonths, exactCharges);
        return schedule(p.method(), rate, BigDecimal.valueOf(rows.size()).setScale(2), base, residual, rows);
    }

    /**
     * Rounds the accumulated depreciation (not each year) to cents, so rounding drift cannot pile up over a long
     * life; the last year takes the remaining residue, so the total equals the base exactly.
     */
    private List<DepreciationRow> buildRows(BigDecimal asset, BigDecimal base,
                                            List<Integer> yearMonths, List<BigDecimal> exactCharges) {
        List<DepreciationRow> rows = new ArrayList<>(exactCharges.size());
        BigDecimal accumulated = BigDecimal.ZERO.setScale(MonetaryRounding.MONETARY_SCALE);
        BigDecimal exactAccumulated = BigDecimal.ZERO;
        for (int i = 0; i < exactCharges.size(); i++) {
            BigDecimal remaining = base.subtract(accumulated);
            exactAccumulated = exactAccumulated.add(exactCharges.get(i), MC);
            BigDecimal charge = i == exactCharges.size() - 1
                    ? remaining
                    : MonetaryRounding.round(exactAccumulated).subtract(accumulated).min(remaining);
            BigDecimal opening = asset.subtract(accumulated);
            accumulated = accumulated.add(charge);
            rows.add(new DepreciationRow(i + 1, yearMonths.get(i), opening, charge, accumulated,
                    asset.subtract(accumulated)));
        }
        return rows;
    }

    private DepreciationSchedule schedule(DepreciationMethod method, BigDecimal ratePercent,
                                          BigDecimal effectiveLife, BigDecimal base, BigDecimal residual,
                                          List<DepreciationRow> rows) {
        DepreciationRow reference = rows.stream().filter(r -> r.months() == 12).findFirst().orElse(rows.getFirst());
        BigDecimal monthly = MonetaryRounding.round(
                reference.depreciation().divide(BigDecimal.valueOf(reference.months()), MC));
        return new DepreciationSchedule(
                method,
                ratePercent.setScale(2, MonetaryRounding.MODE),
                effectiveLife,
                monthly,
                base,
                rows.getLast().accumulatedDepreciation(),
                residual,
                List.copyOf(rows));
    }

    private void validate(DepreciationParameters p) {
        if (p.method() == null) {
            throw new IllegalArgumentException("O método de depreciação é obrigatório.");
        }
        if (p.assetValue() == null || p.assetValue().signum() <= 0) {
            throw new IllegalArgumentException("O valor do ativo deve ser positivo.");
        }
        if (p.residualValue() == null || p.residualValue().signum() < 0) {
            throw new IllegalArgumentException("O valor residual não pode ser negativo.");
        }
        requireBounded(p.assetValue(), MAX_AMOUNT, "O valor do ativo excede o limite permitido.");
        requireBounded(p.residualValue(), MAX_AMOUNT, "O valor residual excede o limite permitido.");
        if (p.residualValue().compareTo(p.assetValue()) >= 0) {
            throw new IllegalArgumentException("O valor residual deve ser menor que o valor do ativo.");
        }
        if (p.usefulLifeYears() < 1 || p.usefulLifeYears() > 50) {
            throw new IllegalArgumentException("A vida útil deve estar entre 1 e 50 anos.");
        }
        shiftCoefficient(p.shiftsPerDay());
        if (p.firstYearMonths() < 1 || p.firstYearMonths() > 12) {
            throw new IllegalArgumentException("Os meses do primeiro ano devem estar entre 1 e 12.");
        }
        if (p.method() == DepreciationMethod.DECLINING_BALANCE
                && (p.decliningFactor().compareTo(BigDecimal.ONE) < 0
                || p.decliningFactor().compareTo(MAX_DECLINING_FACTOR) > 0)) {
            throw new IllegalArgumentException("O fator do saldo decrescente deve estar entre 1 e 4.");
        }
        if (p.method() == DepreciationMethod.UNITS_OF_PRODUCTION) {
            validateUnits(p);
        }
    }

    private void validateUnits(DepreciationParameters p) {
        if (p.shiftsPerDay() != 1) {
            throw new IllegalArgumentException(
                    "A depreciação acelerada por turnos não se aplica ao método de unidades produzidas.");
        }
        if (p.totalUnits() == null || p.totalUnits().signum() <= 0) {
            throw new IllegalArgumentException("O total de unidades deve ser positivo.");
        }
        requireBounded(p.totalUnits(), MAX_UNITS, "O total de unidades excede o limite permitido.");
        if (p.unitsPerYear().isEmpty()) {
            throw new IllegalArgumentException("Informe as unidades produzidas por ano.");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal u : p.unitsPerYear()) {
            if (u == null || u.signum() < 0) {
                throw new IllegalArgumentException("As unidades por ano não podem ser negativas.");
            }
            requireBounded(u, MAX_UNITS, "As unidades por ano excedem o limite permitido.");
            sum = sum.add(u);
        }
        if (sum.compareTo(p.totalUnits()) < 0) {
            throw new IllegalArgumentException(
                    "A soma das unidades por ano deve atingir o total de unidades do ativo.");
        }
    }

    private static void requireBounded(BigDecimal value, BigDecimal max, String message) {
        if (value.compareTo(max) > 0) {
            throw new IllegalArgumentException(message);
        }
        if (value.scale() > MAX_INPUT_SCALE) {
            throw new IllegalArgumentException("Os valores aceitam no máximo " + MAX_INPUT_SCALE + " casas decimais.");
        }
    }
}
