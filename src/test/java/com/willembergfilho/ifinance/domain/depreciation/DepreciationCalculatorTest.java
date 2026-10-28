package com.willembergfilho.ifinance.domain.depreciation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static com.willembergfilho.ifinance.domain.depreciation.DepreciationMethod.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class DepreciationCalculatorTest {

    private final DepreciationCalculator calculator = new DepreciationCalculator();

    private static DepreciationParameters params(String asset, String residual, int life, DepreciationMethod method,
                                                 String factor, int shifts, int firstYearMonths) {
        return new DepreciationParameters(new BigDecimal(asset), new BigDecimal(residual), life, method,
                factor == null ? null : new BigDecimal(factor), null, null, shifts, firstYearMonths);
    }

    private static DepreciationParameters params(String asset, String residual, int life, DepreciationMethod method) {
        return params(asset, residual, life, method, null, 1, 12);
    }

    private static List<String> charges(DepreciationSchedule s) {
        return s.rows().stream().map(r -> r.depreciation().toPlainString()).toList();
    }

    @Test
    void straightLine_10000_residual1000_5years() {
        // (10000 - 1000) / 5 = 1800 per year; rate 1/5 = 20%; 1800 / 12 = 150 per month
        DepreciationSchedule s = calculator.calculate(params("10000", "1000", 5, STRAIGHT_LINE));

        assertThat(charges(s)).containsExactly("1800.00", "1800.00", "1800.00", "1800.00", "1800.00");
        assertThat(s.annualRatePercent()).isEqualByComparingTo("20");
        assertThat(s.effectiveLifeYears()).isEqualByComparingTo("5");
        assertThat(s.monthlyDepreciationFirstFullYear()).isEqualByComparingTo("150");
        assertThat(s.rows().get(1).openingBookValue()).isEqualByComparingTo("8200");
        assertThat(s.rows().get(1).accumulatedDepreciation()).isEqualByComparingTo("3600");
        assertThat(s.rows().getLast().closingBookValue()).isEqualByComparingTo("1000");
        assertThat(s.totalDepreciation()).isEqualByComparingTo("9000");
    }

    @Test
    void sumOfYearsDigits_10000_residual1000_5years() {
        // digits 1+2+3+4+5 = 15; 9000 * 5/15, 4/15, 3/15, 2/15, 1/15; first-year rate 5/15 = 33.33%
        DepreciationSchedule s = calculator.calculate(params("10000", "1000", 5, SUM_OF_YEARS_DIGITS));

        assertThat(charges(s)).containsExactly("3000.00", "2400.00", "1800.00", "1200.00", "600.00");
        assertThat(s.annualRatePercent()).isEqualByComparingTo("33.33");
        assertThat(s.monthlyDepreciationFirstFullYear()).isEqualByComparingTo("250");
    }

    @Test
    void decliningBalance_double_10000_residual1000_5years_stopsAtResidual() {
        // rate 2/5 = 40% on book value: 4000, 2400, 1440, 864; year 5 would be 518.40 but is capped at 1296 - 1000
        DepreciationSchedule s = calculator.calculate(params("10000", "1000", 5, DECLINING_BALANCE));

        assertThat(charges(s)).containsExactly("4000.00", "2400.00", "1440.00", "864.00", "296.00");
        assertThat(s.annualRatePercent()).isEqualByComparingTo("40");
        assertThat(s.rows().getLast().closingBookValue()).isEqualByComparingTo("1000");
    }

    @Test
    void decliningBalance_switchesToStraightLine_noResidual() {
        // Year 4: DB 2160 * 40% = 864 < SL 2160 / 2 = 1080 -> switch, 1080 in years 4 and 5
        DepreciationSchedule s = calculator.calculate(params("10000", "0", 5, DECLINING_BALANCE));

        assertThat(charges(s)).containsExactly("4000.00", "2400.00", "1440.00", "1080.00", "1080.00");
        assertThat(s.rows().getLast().closingBookValue()).isEqualByComparingTo("0");
    }

    @Test
    void decliningBalance_factor150_switchesAndLandsOnResidual() {
        // rate 30%: 3000 (BV 7000), 2100 (BV 4900), 1470 (BV 3430);
        // year 4: DB 1029 < SL (3430 - 1000) / 2 = 1215 -> switch, 1215 twice -> BV 1000
        DepreciationSchedule s = calculator.calculate(params("10000", "1000", 5, DECLINING_BALANCE, "1.5", 1, 12));

        assertThat(charges(s)).containsExactly("3000.00", "2100.00", "1470.00", "1215.00", "1215.00");
        assertThat(s.annualRatePercent()).isEqualByComparingTo("30");
    }

    @Test
    void decliningBalance_rateAbove100Percent_neverGoesBelowResidual() {
        // L = 1, factor 3 -> 300% would overshoot; capped at the depreciable base
        DepreciationSchedule s = calculator.calculate(params("5000", "500", 1, DECLINING_BALANCE, "3", 1, 12));

        assertThat(charges(s)).containsExactly("4500.00");
        assertThat(s.finalBookValue()).isEqualByComparingTo("500");
    }

    @Test
    void shifts_two_applyCoefficient150_shorteningLife() {
        // 1.5 * 20% = 30%/year; life 60 / 1.5 = 40 months; 9000 / 40 = 225/month -> 2700, 2700, 2700, 900 (4 months)
        DepreciationSchedule s = calculator.calculate(params("10000", "1000", 5, STRAIGHT_LINE, null, 2, 12));

        assertThat(charges(s)).containsExactly("2700.00", "2700.00", "2700.00", "900.00");
        assertThat(s.rows().getLast().months()).isEqualTo(4);
        assertThat(s.annualRatePercent()).isEqualByComparingTo("30");
        assertThat(s.effectiveLifeYears()).isEqualByComparingTo("3.33");
    }

    @Test
    void shifts_three_applyCoefficient200() {
        // 2.0 * 20% = 40%/year; life 30 months; 9000 / 30 = 300/month -> 3600, 3600, 1800
        DepreciationSchedule s = calculator.calculate(params("10000", "1000", 5, STRAIGHT_LINE, null, 3, 12));

        assertThat(charges(s)).containsExactly("3600.00", "3600.00", "1800.00");
        assertThat(s.annualRatePercent()).isEqualByComparingTo("40");
        assertThat(s.effectiveLifeYears()).isEqualByComparingTo("2.5");
    }

    @Test
    void shifts_three_decliningBalance_compressesAssetPeriodsToSixMonths() {
        // Periods (6 months each): 4000, 2400, 1440, 1080, 1080 -> fiscal years 6400, 2520, 1080
        DepreciationSchedule s = calculator.calculate(params("10000", "0", 5, DECLINING_BALANCE, null, 3, 12));

        assertThat(charges(s)).containsExactly("6400.00", "2520.00", "1080.00");
        assertThat(s.annualRatePercent()).isEqualByComparingTo("80");
    }

    @Test
    void firstYearProRata_straightLine_extendsIntoExtraPartialYear() {
        // 150/month: 6 months = 900, four full years of 1800, then the remaining 6 months = 900
        DepreciationSchedule s = calculator.calculate(params("10000", "1000", 5, STRAIGHT_LINE, null, 1, 6));

        assertThat(charges(s)).containsExactly("900.00", "1800.00", "1800.00", "1800.00", "1800.00", "900.00");
        assertThat(s.rows().stream().map(DepreciationRow::months).toList()).containsExactly(6, 12, 12, 12, 12, 6);
        assertThat(s.monthlyDepreciationFirstFullYear()).isEqualByComparingTo("150");
    }

    @Test
    void firstYearProRata_sumOfYearsDigits_splitsAssetYearsAcrossFiscalYears() {
        // Asset years 3000, 2400, 1800, 1200, 600 split 6/6 months:
        // 1500 | 1500+1200 | 1200+900 | 900+600 | 600+300 | 300
        DepreciationSchedule s = calculator.calculate(params("10000", "1000", 5, SUM_OF_YEARS_DIGITS, null, 1, 6));

        assertThat(charges(s)).containsExactly("1500.00", "2700.00", "2100.00", "1500.00", "900.00", "300.00");
    }

    @Test
    void residualExactInCents_roundsAccumulatedAndLastPeriodTakesResidue() {
        // Accumulated 333.333 -> 333.33, 666.667 -> 666.67 (charge 333.34); last year takes 1000 - 666.67 = 333.33
        DepreciationSchedule s = calculator.calculate(params("1000", "0", 3, STRAIGHT_LINE));

        assertThat(charges(s)).containsExactly("333.33", "333.34", "333.33");
        assertThat(s.totalDepreciation()).isEqualByComparingTo("1000");
    }

    @Test
    void roundingDrift_doesNotPileUpInLastPartialYear() {
        // 100 / 360 months: 7 months = 1.9444 -> 1.94; 29 full years of 3.3333 each; accumulated after 355 months
        // = 98.6111 -> 98.61, so the last 5 months get 1.39 (the exact 1.3889), not 1.49 from per-year rounding.
        DepreciationSchedule s = calculator.calculate(params("100", "0", 30, STRAIGHT_LINE, null, 1, 7));

        assertThat(s.rows()).hasSize(31);
        assertThat(s.rows().getFirst().depreciation()).isEqualByComparingTo("1.94");
        assertThat(s.rows().getLast().months()).isEqualTo(5);
        assertThat(s.rows().getLast().depreciation()).isEqualByComparingTo("1.39");
        assertThat(s.totalDepreciation()).isEqualByComparingTo("100");
    }

    @Test
    void residualExactInCents_acrossManyCombinations() {
        String[][] values = {{"10000", "1000"}, {"1234.57", "0.01"}, {"999999.99", "123.45"}, {"0.10", "0"}};
        for (String[] v : values) {
            for (DepreciationMethod m : List.of(STRAIGHT_LINE, SUM_OF_YEARS_DIGITS, DECLINING_BALANCE)) {
                for (int life : new int[]{1, 3, 7, 50}) {
                    for (int shifts = 1; shifts <= 3; shifts++) {
                        for (int first : new int[]{1, 7, 12}) {
                            DepreciationSchedule s = calculator.calculate(params(v[0], v[1], life, m, null, shifts, first));
                            BigDecimal base = new BigDecimal(v[0]).subtract(new BigDecimal(v[1]));
                            BigDecimal sum = s.rows().stream().map(DepreciationRow::depreciation)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            assertThat(sum).isEqualByComparingTo(base);
                            assertThat(s.rows().getLast().closingBookValue()).isEqualByComparingTo(v[1]);
                            assertThat(s.rows()).allSatisfy(r -> {
                                assertThat(r.depreciation().signum()).isGreaterThanOrEqualTo(0);
                                assertThat(r.closingBookValue()).isGreaterThanOrEqualTo(new BigDecimal(v[1]));
                                assertThat(r.depreciation().scale()).isEqualTo(2);
                            });
                        }
                    }
                }
            }
        }
    }

    @Test
    void unitsOfProduction_proportionalToOutput() {
        // 9000 / 100000 units = 0.09 per unit: 30000 -> 2700, 25000 -> 2250, 20000 -> 1800, 15000 -> 1350, 10000 -> 900
        DepreciationParameters p = new DepreciationParameters(new BigDecimal("10000"), new BigDecimal("1000"), 5,
                UNITS_OF_PRODUCTION, null, new BigDecimal("100000"),
                List.of(new BigDecimal("30000"), new BigDecimal("25000"), new BigDecimal("20000"),
                        new BigDecimal("15000"), new BigDecimal("10000")), 1, 12);

        DepreciationSchedule s = calculator.calculate(p);

        assertThat(charges(s)).containsExactly("2700.00", "2250.00", "1800.00", "1350.00", "900.00");
        assertThat(s.annualRatePercent()).isEqualByComparingTo("30");
    }

    @Test
    void unitsOfProduction_stopsWhenTotalUnitsReached() {
        // 60000 -> 5400; second year only 40000 units remain -> 3600; third year is not emitted
        DepreciationParameters p = new DepreciationParameters(new BigDecimal("10000"), new BigDecimal("1000"), 5,
                UNITS_OF_PRODUCTION, null, new BigDecimal("100000"),
                List.of(new BigDecimal("60000"), new BigDecimal("60000"), new BigDecimal("60000")), 1, 12);

        assertThat(charges(calculator.calculate(p))).containsExactly("5400.00", "3600.00");
    }

    @Test
    void unitsOfProduction_rejectsInsufficientUnitsAndShifts() {
        DepreciationParameters shortPlan = new DepreciationParameters(new BigDecimal("10000"), BigDecimal.ZERO, 5,
                UNITS_OF_PRODUCTION, null, new BigDecimal("100"), List.of(new BigDecimal("50")), 1, 12);
        DepreciationParameters withShifts = new DepreciationParameters(new BigDecimal("10000"), BigDecimal.ZERO, 5,
                UNITS_OF_PRODUCTION, null, new BigDecimal("100"), List.of(new BigDecimal("100")), 2, 12);

        assertThatThrownBy(() -> calculator.calculate(shortPlan)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(withShifts)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidInputs() {
        assertThatThrownBy(() -> calculator.calculate(params("1000", "1000", 5, STRAIGHT_LINE)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("residual");
        assertThatThrownBy(() -> calculator.calculate(params("0", "0", 5, STRAIGHT_LINE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(params("1000", "0", 51, STRAIGHT_LINE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(params("1000", "0", 5, STRAIGHT_LINE, null, 4, 12)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(params("1000", "0", 5, STRAIGHT_LINE, null, 1, 13)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(params("1000", "0", 5, DECLINING_BALANCE, "0.5", 1, 12)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void subCentDifference_reportsMinimumDifferenceNotResidualMessage() {
        // 0.001 rounds to 0.00 and 999.999 rounds to 1000.00: both leave a zero base after rounding to cents
        assertThatThrownBy(() -> calculator.calculate(params("0.001", "0", 5, STRAIGHT_LINE)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("R$ 0,01");
        assertThatThrownBy(() -> calculator.calculate(params("1000", "999.999", 5, STRAIGHT_LINE)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("R$ 0,01");
    }

    @Test
    void hugeOrTinyExponents_areRejectedBeforeExactArithmetic() {
        // Without bounds, setScale/add on 1E+30000000 or 1E-30000000 builds a 30-million-digit BigInteger.
        DepreciationParameters hugeUnits = new DepreciationParameters(new BigDecimal("10000"), BigDecimal.ZERO, 5,
                UNITS_OF_PRODUCTION, null, new BigDecimal("1E+30000000"),
                List.of(new BigDecimal("0.5"), new BigDecimal("1E+30000000")), 1, 12);
        DepreciationParameters tinyUnits = new DepreciationParameters(new BigDecimal("10000"), BigDecimal.ZERO, 5,
                UNITS_OF_PRODUCTION, null, new BigDecimal("100"),
                List.of(new BigDecimal("100"), new BigDecimal("1E-30000000")), 1, 12);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertThatThrownBy(() -> calculator.calculate(params("100", "1E+30000000", 5, STRAIGHT_LINE)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("limite");
            assertThatThrownBy(() -> calculator.calculate(params("100", "1E-30000000", 5, STRAIGHT_LINE)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("casas decimais");
            assertThatThrownBy(() -> calculator.calculate(hugeUnits))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("limite");
            assertThatThrownBy(() -> calculator.calculate(tinyUnits))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("casas decimais");
        });
    }
}
