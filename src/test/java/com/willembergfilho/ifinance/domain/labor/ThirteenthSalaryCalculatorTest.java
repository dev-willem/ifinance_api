package com.willembergfilho.ifinance.domain.labor;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThirteenthSalaryCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void fullYear_3000() {
        // first 1500; INSS 248.60; IRRF simplified base 2392.80 exempt; second = 1500 - 248.60 = 1251.40
        ThirteenthSalaryResult r = ThirteenthSalaryCalculator.calculate(
                new ThirteenthSalaryParameters(bd("3000"), null, 12, 0, 2026));

        assertThat(r.grossTotal()).isEqualByComparingTo("3000.00");
        assertThat(r.firstInstallment()).isEqualByComparingTo("1500.00");
        assertThat(r.inss().total()).isEqualByComparingTo("248.60");
        assertThat(r.irrf().tax()).isEqualByComparingTo("0");
        assertThat(r.secondInstallment()).isEqualByComparingTo("1251.40");
        assertThat(r.netTotal()).isEqualByComparingTo("2751.40");
    }

    @Test
    void fullYear_6500_oneDependent_partialReduction() {
        // INSS 411.1122 + 2145.73 x 14% (300.4022) = 711.51; legal 711.51 + 189.59 = 901.10
        // base 5598.90 x 27.5% - 908.73 = 630.97; reduction 978.62 - 0.133145 x 6500 = 113.18 -> IRRF 517.79
        ThirteenthSalaryResult r = ThirteenthSalaryCalculator.calculate(
                new ThirteenthSalaryParameters(bd("6500"), null, 12, 1, 2026));

        assertThat(r.inss().total()).isEqualByComparingTo("711.51");
        assertThat(r.irrf().taxBeforeReduction()).isEqualByComparingTo("630.97");
        assertThat(r.irrf().reduction()).isEqualByComparingTo("113.18");
        assertThat(r.irrf().tax()).isEqualByComparingTo("517.79");
        assertThat(r.secondInstallment()).isEqualByComparingTo("2020.70"); // 3250 - 711.51 - 517.79
        assertThat(r.netTotal()).isEqualByComparingTo("5270.70");
    }

    @Test
    void sameSalaryIn2025_hasNoReduction() {
        ThirteenthSalaryResult r = ThirteenthSalaryCalculator.calculate(
                new ThirteenthSalaryParameters(bd("6500"), null, 12, 1, 2025));

        assertThat(r.irrf().reduction()).isEqualByComparingTo("0");
    }

    @Test
    void proportionalFiveMonths() {
        // 4801.20 x 5/12 = 2000.50; first 1000.25; INSS 121.575 + 379.50 x 9% (34.155) = 155.73
        ThirteenthSalaryResult r = ThirteenthSalaryCalculator.calculate(
                new ThirteenthSalaryParameters(bd("4801.20"), null, 5, 0, 2026));

        assertThat(r.grossTotal()).isEqualByComparingTo("2000.50");
        assertThat(r.firstInstallment()).isEqualByComparingTo("1000.25");
        assertThat(r.inss().total()).isEqualByComparingTo("155.73");
        assertThat(r.secondInstallment()).isEqualByComparingTo("844.52");
    }

    @Test
    void monthsOutOfRange_rejected() {
        assertThatThrownBy(() -> new ThirteenthSalaryParameters(bd("3000"), null, 0, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ThirteenthSalaryParameters(bd("3000"), null, 13, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
