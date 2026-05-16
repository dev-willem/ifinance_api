package com.willembergfilho.ifinance.domain.math;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class RateConverterTest {

    private RateConverter converter;

    @BeforeEach
    void setUp() {
        converter = new RateConverter();
    }

    // ------------------------------------------------- nominalToEffective ---

    @Test
    void nominalToEffective_12pct_monthly_approx12_68pct() {
        BigDecimal nominal = new BigDecimal("0.12");
        BigDecimal effective = converter.nominalToEffective(nominal, 12);

        // (1 + 0.12/12)^12 - 1 = (1.01)^12 - 1 ≈ 0.12682503...
        BigDecimal expected = new BigDecimal("0.1268");
        BigDecimal actual = effective.setScale(4, RoundingMode.HALF_EVEN);
        assertEquals(expected, actual);
    }

    @Test
    void nominalToEffective_zeroRate_returnsZero() {
        BigDecimal effective = converter.nominalToEffective(BigDecimal.ZERO, 12);

        assertEquals(0, effective.compareTo(BigDecimal.ZERO));
    }

    @Test
    void nominalToEffective_singleCompoundingPeriod_equalsNominal() {
        BigDecimal nominal = new BigDecimal("0.10");
        BigDecimal effective = converter.nominalToEffective(nominal, 1);

        assertEquals(0, nominal.compareTo(effective.stripTrailingZeros()));
    }

    // ---------------------------------------------------- toPeriodicRate ---

    @Test
    void toPeriodicRate_roundTrip_fromEffectiveAnnual() {
        BigDecimal nominalAnnual = new BigDecimal("0.12");
        BigDecimal effectiveAnnual = converter.nominalToEffective(nominalAnnual, 12);
        BigDecimal periodicRate = converter.toPeriodicRate(effectiveAnnual, 12);

        // Should be close to 0.01 (1% monthly)
        BigDecimal expected = new BigDecimal("0.01");
        BigDecimal diff = periodicRate.subtract(expected).abs();
        assertTrue(diff.compareTo(new BigDecimal("0.0000001")) < 0,
                "Round-trip periodic rate should be ≈ 0.01, got: " + periodicRate);
    }

    @Test
    void toPeriodicRate_10pctAnnual_monthly() {
        BigDecimal annual = new BigDecimal("0.10");
        BigDecimal monthly = converter.toPeriodicRate(annual, 12);

        // (1.10)^(1/12) - 1 ≈ 0.007974...
        assertTrue(monthly.compareTo(new BigDecimal("0.007")) > 0);
        assertTrue(monthly.compareTo(new BigDecimal("0.009")) < 0);
    }

    @Test
    void toEffectiveAnnual_1pctMonthly_approx12_68pct() {
        BigDecimal monthly = new BigDecimal("0.01");
        BigDecimal annual = converter.toEffectiveAnnual(monthly, 12);

        // (1.01)^12 - 1 ≈ 0.12682503...
        BigDecimal rounded = annual.setScale(4, RoundingMode.HALF_EVEN);
        assertEquals(new BigDecimal("0.1268"), rounded);
    }

    // --------------------------------------------------------- realRate ---

    @Test
    void realRate_fisher_10pctNominal_5pctInflation() {
        BigDecimal nominal = new BigDecimal("0.10");
        BigDecimal inflation = new BigDecimal("0.05");
        BigDecimal real = converter.realRate(nominal, inflation);

        // (1.10 / 1.05) - 1 = 0.047619...
        BigDecimal rounded = real.setScale(4, RoundingMode.HALF_EVEN);
        assertEquals(new BigDecimal("0.0476"), rounded);
    }

    @Test
    void realRate_zeroInflation_equalsNominal() {
        BigDecimal nominal = new BigDecimal("0.08");
        BigDecimal real = converter.realRate(nominal, BigDecimal.ZERO);

        assertEquals(0, nominal.compareTo(real.setScale(2, RoundingMode.HALF_EVEN)));
    }

    @Test
    void realRate_inflationEqualsNominal_closerToZero() {
        BigDecimal rate = new BigDecimal("0.05");
        BigDecimal real = converter.realRate(rate, rate);

        // (1.05/1.05) - 1 = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(real.stripTrailingZeros()));
    }

    @Test
    void realRate_highInflation_negativeReal() {
        BigDecimal nominal = new BigDecimal("0.05");
        BigDecimal inflation = new BigDecimal("0.10");
        BigDecimal real = converter.realRate(nominal, inflation);

        assertTrue(real.compareTo(BigDecimal.ZERO) < 0,
                "Real rate should be negative when inflation > nominal");
    }
}
