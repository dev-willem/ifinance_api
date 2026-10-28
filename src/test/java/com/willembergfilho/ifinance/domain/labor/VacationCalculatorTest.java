package com.willembergfilho.ifinance.domain.labor;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VacationCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void thirtyDays_noAbono() {
        // 3000 + 1/3 1000 = 4000 taxable; INSS 368.60; IRRF simplified base 3392.80 -> 114.76, fully reduced (<= 5000)
        VacationResult r = VacationCalculator.calculate(new VacationParameters(bd("3000"), null, 30, 0, 0, 2026));

        assertThat(r.vacationPay()).isEqualByComparingTo("3000.00");
        assertThat(r.vacationBonus()).isEqualByComparingTo("1000.00");
        assertThat(r.taxableTotal()).isEqualByComparingTo("4000.00");
        assertThat(r.inss().total()).isEqualByComparingTo("368.60");
        assertThat(r.irrf().taxBeforeReduction()).isEqualByComparingTo("114.76");
        assertThat(r.irrf().tax()).isEqualByComparingTo("0");
        assertThat(r.netTotal()).isEqualByComparingTo("3631.40");
    }

    @Test
    void twentyDaysPlusTenSold_abonoExempt() {
        // 6000/30 = 200/day: férias 4000 + 1/3 1333.33 = 5333.33 taxable; abono 2000 + 1/3 666.67 exempt
        // INSS: 411.1122 + 979.06 x 14% (137.0684) = 548.18
        // IRRF: simplified base 4726.13 x 27.5% - 908.73 = 390.96; reduction 978.62 - 0.133145 x 5333.33 = 268.51 -> 122.45
        VacationResult r = VacationCalculator.calculate(new VacationParameters(bd("6000"), null, 20, 10, 0, 2026));

        assertThat(r.allowance()).isEqualByComparingTo("2000.00");
        assertThat(r.allowanceBonus()).isEqualByComparingTo("666.67");
        assertThat(r.taxableTotal()).isEqualByComparingTo("5333.33");
        assertThat(r.inss().total()).isEqualByComparingTo("548.18");
        assertThat(r.irrf().reduction()).isEqualByComparingTo("268.51");
        assertThat(r.irrf().tax()).isEqualByComparingTo("122.45");
        assertThat(r.grossTotal()).isEqualByComparingTo("8000.00");
        assertThat(r.netTotal()).isEqualByComparingTo("7329.37");
        assertThat(r.lines()).filteredOn(l -> Boolean.FALSE.equals(l.taxable())).hasSize(2);
    }

    @Test
    void variablePayIncludedInRemuneration() {
        // (2000 + 400) x 30/30 = 2400 + 800 = 3200; INSS 121.575 + 115.3656 + 297.16 x 12% (35.6592) = 272.60
        VacationResult r = VacationCalculator.calculate(new VacationParameters(bd("2000"), bd("400"), 30, 0, 0, 2026));

        assertThat(r.remuneration()).isEqualByComparingTo("2400.00");
        assertThat(r.taxableTotal()).isEqualByComparingTo("3200.00");
        assertThat(r.inss().total()).isEqualByComparingTo("272.60");
    }

    @Test
    void abonoAboveOneThird_rejected() {
        // 15 + 10 = 25 entitled days: at most 8 may be sold
        assertThatThrownBy(() -> new VacationParameters(bd("3000"), null, 15, 10, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1/3");
    }

    @Test
    void moreThanThirtyDays_rejected() {
        assertThatThrownBy(() -> new VacationParameters(bd("3000"), null, 25, 10, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VacationParameters(bd("3000"), null, 4, 0, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
