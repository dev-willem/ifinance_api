package com.willembergfilho.ifinance.domain.math;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class NpvCalculatorTest {

    private final NpvCalculator npv = new NpvCalculator();

    private static List<BigDecimal> flows(String... values) {
        return java.util.Arrays.stream(values).map(BigDecimal::new).toList();
    }

    @Test
    void annuityDiscountedAtTenPercent() {
        // -1000 + 500/1.1 + 500/1.21 + 500/1.331 = -1000 + 454.5455 + 413.2231 + 375.6574 = 243.4260
        assertThat(npv.calculate(flows("-1000", "500", "500", "500"), new BigDecimal("0.10")))
                .isCloseTo(new BigDecimal("243.425995492"), within(new BigDecimal("0.000001")));
    }

    @Test
    void npvIsZeroAtTheIrr() {
        // -1000 + 1100/1.1 = 0
        assertThat(npv.calculate(flows("-1000", "1100"), new BigDecimal("0.10")))
                .isCloseTo(BigDecimal.ZERO, within(new BigDecimal("1e-20")));
    }

    @Test
    void zeroRateIsPlainSum() {
        assertThat(npv.calculate(flows("-1000", "500", "500", "500"), BigDecimal.ZERO)).isEqualByComparingTo("500");
    }

    @Test
    void derivativeMatchesClosedForm() {
        // d/dr [-1000 + 1100/(1+r)] = -1100/(1+r)^2 = -1100/1.21 = -909.0909...
        assertThat(npv.derivative(flows("-1000", "1100"), new BigDecimal("0.10")))
                .isCloseTo(new BigDecimal("-909.090909"), within(new BigDecimal("0.000001")));
    }
}
