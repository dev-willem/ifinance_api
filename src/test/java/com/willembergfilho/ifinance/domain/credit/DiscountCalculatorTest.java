package com.willembergfilho.ifinance.domain.credit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountCalculatorTest {

    private final DiscountCalculator calculator = new DiscountCalculator();

    private List<DiscountResult> calc(String n, String i, String periods) {
        return calculator.calculate(new DiscountParameters(new BigDecimal(n), new BigDecimal(i), new BigDecimal(periods)));
    }

    private static DiscountResult of(List<DiscountResult> results, DiscountModality modality) {
        return results.stream().filter(r -> r.modality() == modality).findFirst().orElseThrow();
    }

    // N = 1000, i = 5%, n = 4 for all four modalities

    @Test
    void rationalSimple() {
        // A = 1000 / (1 + 0.05 x 4) = 833.33; effective per period (simple) = i; compound = 1.2^(1/4) - 1
        DiscountResult r = of(calc("1000", "0.05", "4"), DiscountModality.RATIONAL_SIMPLE);

        assertThat(r.valid()).isTrue();
        assertThat(r.presentValue()).isEqualByComparingTo("833.33");
        assertThat(r.discountAmount()).isEqualByComparingTo("166.67");
        assertThat(r.effectiveRateTotal()).isEqualByComparingTo("0.20");
        assertThat(r.effectiveSimpleRatePerPeriod()).isEqualByComparingTo("0.05");
        assertThat(r.effectiveCompoundRatePerPeriod()).isEqualByComparingTo("0.0466351394");
    }

    @Test
    void commercialSimple_effectiveRateExceedsNominal() {
        // D = 1000 x 0.05 x 4 = 200; A = 800; total = 200/800 = 25%; simple per period = 0.05 / (1 - 0.2) = 6.25%
        DiscountResult r = of(calc("1000", "0.05", "4"), DiscountModality.COMMERCIAL_SIMPLE);

        assertThat(r.valid()).isTrue();
        assertThat(r.presentValue()).isEqualByComparingTo("800.00");
        assertThat(r.discountAmount()).isEqualByComparingTo("200.00");
        assertThat(r.effectiveRateTotal()).isEqualByComparingTo("0.25");
        assertThat(r.effectiveSimpleRatePerPeriod()).isEqualByComparingTo("0.0625");
        assertThat(r.effectiveCompoundRatePerPeriod()).isEqualByComparingTo("0.0573712634");
    }

    @Test
    void rationalCompound() {
        // A = 1000 / 1.05^4 = 1000 / 1.21550625 = 822.70; effective compound per period = i
        DiscountResult r = of(calc("1000", "0.05", "4"), DiscountModality.RATIONAL_COMPOUND);

        assertThat(r.presentValue()).isEqualByComparingTo("822.70");
        assertThat(r.discountAmount()).isEqualByComparingTo("177.30");
        assertThat(r.effectiveCompoundRatePerPeriod()).isEqualByComparingTo("0.05");
    }

    @Test
    void commercialCompound() {
        // A = 1000 x 0.95^4 = 814.50625 -> 814.51; effective compound per period = 1/0.95 - 1 = 5.2631578947%
        DiscountResult r = of(calc("1000", "0.05", "4"), DiscountModality.COMMERCIAL_COMPOUND);

        assertThat(r.presentValue()).isEqualByComparingTo("814.51");
        assertThat(r.discountAmount()).isEqualByComparingTo("185.49");
        assertThat(r.effectiveCompoundRatePerPeriod()).isEqualByComparingTo("0.0526315789");
    }

    @Test
    void resultsComeInFixedOrder() {
        assertThat(calc("1000", "0.05", "4")).extracting(DiscountResult::modality).containsExactly(
                DiscountModality.RATIONAL_SIMPLE, DiscountModality.COMMERCIAL_SIMPLE,
                DiscountModality.RATIONAL_COMPOUND, DiscountModality.COMMERCIAL_COMPOUND);
    }

    @Test
    void fractionalPeriods() {
        // A = 1000 / (1 + 0.02 x 1.5) = 1000 / 1.03 = 970.87
        DiscountResult r = of(calc("1000", "0.02", "1.5"), DiscountModality.RATIONAL_SIMPLE);

        assertThat(r.presentValue()).isEqualByComparingTo("970.87");
    }

    @Test
    void commercialSimple_invalidWhenRateTimesTermReachesOne_othersStillComputed() {
        // i n = 0.10 x 10 = 1 -> A would be zero
        List<DiscountResult> results = calc("1000", "0.10", "10");
        DiscountResult cs = of(results, DiscountModality.COMMERCIAL_SIMPLE);

        assertThat(cs.valid()).isFalse();
        assertThat(cs.presentValue()).isNull();
        assertThat(cs.message()).contains("i·n");
        // rational simple: 1000 / 2 = 500
        assertThat(of(results, DiscountModality.RATIONAL_SIMPLE).presentValue()).isEqualByComparingTo("500.00");
        assertThat(of(results, DiscountModality.COMMERCIAL_COMPOUND).valid()).isTrue();
    }

    @Test
    void commercialCompound_invalidWhenRateIsHundredPercentOrMore() {
        List<DiscountResult> results = calc("1000", "1", "0.5");

        assertThat(of(results, DiscountModality.COMMERCIAL_COMPOUND).valid()).isFalse();
        // rational compound: 1000 / 2^0.5 = 707.11
        assertThat(of(results, DiscountModality.RATIONAL_COMPOUND).presentValue()).isEqualByComparingTo("707.11");
    }

    @Test
    void overflow_marksModalityInvalid() {
        // 11^1200 overflows double
        DiscountResult r = of(calc("1000", "10", "1200"), DiscountModality.RATIONAL_COMPOUND);

        assertThat(r.valid()).isFalse();
    }

    @Test
    void commercialSimple_nearOneRateTimesTerm_marksInvalidInsteadOfFailing() {
        // i n = 10 x 0.0999...9 = 1 - 1e-34: ratio 1e34, PV rounds to 0.00 and ratio^(1/n) overflows double
        List<DiscountResult> results = calc("1000", "10", "0.09999999999999999999999999999999999");
        DiscountResult cs = of(results, DiscountModality.COMMERCIAL_SIMPLE);

        assertThat(cs.valid()).isFalse();
        assertThat(cs.message()).contains("fora da faixa");
        assertThat(of(results, DiscountModality.RATIONAL_SIMPLE).valid()).isTrue();
    }

    @Test
    void commercialSimple_hugeButFiniteEffectiveRate_marksInvalid() {
        // i n = 1 - 1e-30: ratio 1e30 -> PV rounds to 0.00 (would have returned a ~300-digit effective rate)
        DiscountResult cs = of(calc("1000", "10", "0.0999999999999999999999999999999"), DiscountModality.COMMERCIAL_SIMPLE);

        assertThat(cs.valid()).isFalse();
        assertThat(cs.effectiveCompoundRatePerPeriod()).isNull();
    }

    @Test
    void commercialSimple_effectiveRateAboveCap_marksInvalid() {
        // N = 1e11, i n = 0.99999 (i = 10, n = 0.099999): ratio 1e5, PV = 1e6 > 0, but ratio^(1/n) = 1e5^10.0001 >> 1e6
        DiscountResult cs = of(calc("100000000000", "10", "0.099999"), DiscountModality.COMMERCIAL_SIMPLE);

        assertThat(cs.valid()).isFalse();
    }

    @Test
    void nonPositiveInputs_areRejected() {
        assertThatThrownBy(() -> calc("0", "0.05", "4")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc("1000", "0", "4")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc("1000", "0.05", "0")).isInstanceOf(IllegalArgumentException.class);
    }
}
