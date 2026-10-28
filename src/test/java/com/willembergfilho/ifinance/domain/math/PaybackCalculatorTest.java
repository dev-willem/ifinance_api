package com.willembergfilho.ifinance.domain.math;

import com.willembergfilho.ifinance.domain.math.PaybackCalculator.PaybackResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PaybackCalculatorTest {

    private final PaybackCalculator calculator = new PaybackCalculator();

    private static List<BigDecimal> flows(String... values) {
        return Arrays.stream(values).map(BigDecimal::new).toList();
    }

    @Test
    void simpleAndDiscountedPaybackWithInterpolation() {
        // simple: cumulative -400 after t=1, +200 after t=2 -> 1 + 400/600 = 1.67
        // discounted @10%: 545.4545 then 495.8678; cumulative -454.5455 -> 1 + 454.5455/495.8678 = 1.9167 -> 1.92
        PaybackResult r = calculator.calculate(flows("-1000", "600", "600"), new BigDecimal("0.10"));

        assertThat(r.paybackAchieved()).isTrue();
        assertThat(r.simplePayback()).isEqualByComparingTo("1.67");
        assertThat(r.simplePaybackPeriod()).isEqualTo(2);
        assertThat(r.discountedPayback()).isEqualByComparingTo("1.92");
        assertThat(r.discountedPaybackPeriod()).isEqualTo(2);
    }

    @Test
    void exactRecoveryAtPeriodEnd() {
        // -1000 + 500 + 500 = 0 exactly at t=2
        PaybackResult r = calculator.calculate(flows("-1000", "500", "500"), null);

        assertThat(r.simplePayback()).isEqualByComparingTo("2.00");
        assertThat(r.simplePaybackPeriod()).isEqualTo(2);
    }

    @Test
    void discountedNeverRecoversEvenThoughSimpleDoes() {
        // simple: -600, -200, +200 -> 2 + 200/400 = 2.50
        // discounted @10%: 363.64 + 330.58 + 300.53 = 994.74 < 1000 -> never
        PaybackResult r = calculator.calculate(flows("-1000", "400", "400", "400"), new BigDecimal("0.10"));

        assertThat(r.paybackAchieved()).isTrue();
        assertThat(r.simplePayback()).isEqualByComparingTo("2.50");
        assertThat(r.simplePaybackPeriod()).isEqualTo(3);
        assertThat(r.discountedPayback()).isNull();
        assertThat(r.discountedPaybackPeriod()).isNull();
    }

    @Test
    void neverRecovers() {
        PaybackResult r = calculator.calculate(flows("-1000", "100", "100"), new BigDecimal("0.05"));

        assertThat(r.paybackAchieved()).isFalse();
        assertThat(r.simplePayback()).isNull();
        assertThat(r.simplePaybackPeriod()).isNull();
        assertThat(r.discountedPayback()).isNull();
    }

    @Test
    void withoutDiscountRateDiscountedFieldsAreNull() {
        PaybackResult r = calculator.calculate(flows("-100", "150"), null);

        assertThat(r.simplePayback()).isEqualByComparingTo("0.67");
        assertThat(r.discountedPayback()).isNull();
        assertThat(r.discountedPaybackPeriod()).isNull();
    }
}
