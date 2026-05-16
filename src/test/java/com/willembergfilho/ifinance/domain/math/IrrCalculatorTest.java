package com.willembergfilho.ifinance.domain.math;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IrrCalculatorTest {

    private IrrCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new IrrCalculator();
    }

    // ------------------------------------------------- simple cases ---

    @Test
    void irr_simpleOneperiod_10pct() {
        // [-100, 110] → IRR = 10%
        List<BigDecimal> cashFlows = List.of(
                new BigDecimal("-100"),
                new BigDecimal("110")
        );
        BigDecimal irr = calculator.calculate(cashFlows);

        BigDecimal diff = irr.subtract(new BigDecimal("0.10")).abs();
        assertTrue(diff.compareTo(new BigDecimal("0.0001")) < 0,
                "IRR should be ≈ 10%, got: " + irr);
    }

    @Test
    void irr_fourEqualInflows_positive() {
        // [-1000, 300, 300, 300, 300] → positive IRR (< 7.7%)
        List<BigDecimal> cashFlows = List.of(
                new BigDecimal("-1000"),
                new BigDecimal("300"),
                new BigDecimal("300"),
                new BigDecimal("300"),
                new BigDecimal("300")
        );
        BigDecimal irr = calculator.calculate(cashFlows);

        assertTrue(irr.compareTo(BigDecimal.ZERO) > 0,
                "IRR should be positive, got: " + irr);
        assertTrue(irr.compareTo(new BigDecimal("0.10")) < 0,
                "IRR should be less than 10%, got: " + irr);
    }

    @Test
    void irr_exactTenPctCase_returnsApprox10pct() {
        // Construct cash flows whose exact IRR is 10%:
        // [-1000, 1100] → IRR = 10%
        List<BigDecimal> cashFlows = List.of(
                new BigDecimal("-1000"),
                new BigDecimal("1100")
        );
        BigDecimal irr = calculator.calculate(cashFlows);

        BigDecimal diff = irr.subtract(new BigDecimal("0.10")).abs();
        assertTrue(diff.compareTo(new BigDecimal("0.0001")) < 0,
                "IRR should be ≈ 10%, got: " + irr);
    }

    @Test
    void irr_morePeriods_converges() {
        // [-500, 100, 150, 200, 200] — mixed flows, should converge
        List<BigDecimal> cashFlows = List.of(
                new BigDecimal("-500"),
                new BigDecimal("100"),
                new BigDecimal("150"),
                new BigDecimal("200"),
                new BigDecimal("200")
        );
        BigDecimal irr = calculator.calculate(cashFlows);

        assertNotNull(irr);
        assertTrue(irr.compareTo(BigDecimal.ZERO) > 0,
                "IRR should be positive for net-positive flows, got: " + irr);
    }

    @Test
    void irr_zeroNetGain_returnsZero() {
        // [-100, 100] → IRR = 0%
        List<BigDecimal> cashFlows = List.of(
                new BigDecimal("-100"),
                new BigDecimal("100")
        );
        BigDecimal irr = calculator.calculate(cashFlows);

        BigDecimal diff = irr.abs();
        assertTrue(diff.compareTo(new BigDecimal("0.0001")) < 0,
                "IRR should be ≈ 0%, got: " + irr);
    }

    @Test
    void irr_resultScaleIsRateScale() {
        List<BigDecimal> cashFlows = List.of(
                new BigDecimal("-100"),
                new BigDecimal("110")
        );
        BigDecimal irr = calculator.calculate(cashFlows);

        assertEquals(MonetaryRounding.RATE_SCALE, irr.scale());
    }
}
