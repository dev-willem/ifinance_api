package com.willembergfilho.ifinance.domain.math;

import com.willembergfilho.ifinance.domain.simulation.Installment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class InflationCorrectionEngineTest {

    private final InflationCorrectionEngine engine = new InflationCorrectionEngine();
    private final AmortizationEngine amortization = new AmortizationEngine();

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void correctsBalanceEachPeriodBeforeInterestAndAmortization() {
        // 1000 over 2 periods, 1% interest, 10% inflation per period:
        // P1: CM 1000*0.10 = 100.00 -> 1100.00; interest 11.00; amort 1100/2 = 550.00; total 561.00; after 550.00
        // P2: CM 550*0.10 = 55.00 -> 605.00; interest 6.05; amort 605.00 (last); total 611.05; after 0
        List<Installment> base = amortization.calculateSac(bd("1000.00"), bd("0.01"), 2);

        List<Installment> corrected = engine.applyCorrection(base, bd("0.01"), List.of(bd("0.10"), bd("0.10")));

        assertThat(corrected).containsExactly(
                new Installment(1, bd("1100.00"), bd("550.00"), bd("11.00"), bd("100.00"), bd("561.00"), bd("550.00")),
                new Installment(2, bd("605.00"), bd("605.00"), bd("6.05"), bd("55.00"), bd("611.05"), BigDecimal.ZERO));
    }

    @Test
    void zeroInflationReproducesSac() {
        List<Installment> sac = amortization.calculateSac(bd("1200.00"), bd("0.01"), 3);

        List<Installment> corrected = engine.applyCorrection(sac, bd("0.01"), List.of(BigDecimal.ZERO));

        assertThat(corrected).extracting(Installment::total)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyElementsOf(sac.stream().map(Installment::total).toList());
        assertThat(corrected).extracting(Installment::additionalCharges)
                .allSatisfy(cm -> assertThat(cm).isEqualByComparingTo("0"));
    }

    @Test
    void missingRatesFallBackToLastKnownRate() {
        // a rates list shorter than the term, or with a null hole, resolves to the last element (0.005)
        // P1 1200 -> CM 6.00; P2 804 -> CM 4.02; P3 404.01 -> CM 2.02005 -> 2.02 (HALF_EVEN)
        List<Installment> base = amortization.calculateSac(bd("1200.00"), bd("0.01"), 3);

        List<Installment> shortList = engine.applyCorrection(base, bd("0.01"), List.of(bd("0.005")));
        List<Installment> withHole = engine.applyCorrection(base, bd("0.01"), Arrays.asList(bd("0.005"), null, bd("0.005")));

        assertThat(shortList).extracting(Installment::additionalCharges)
                .containsExactly(bd("6.00"), bd("4.02"), bd("2.02"));
        assertThat(withHole).isEqualTo(shortList);
        assertThat(shortList.getLast().principalBalanceAfter()).isEqualByComparingTo("0");
    }

    @Test
    void sumOfAmortizationsEqualsPrincipalPlusMonetaryCorrection() {
        List<Installment> base = amortization.calculatePrice(bd("50000.00"), bd("0.008"), 24);

        List<Installment> corrected = engine.applyCorrection(base, bd("0.008"), List.of(bd("0.0045")));

        BigDecimal amortized = corrected.stream().map(Installment::amortization).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cm = corrected.stream().map(Installment::additionalCharges).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(amortized).isEqualByComparingTo(bd("50000.00").add(cm));
    }

    @Test
    void rejectsEmptyScheduleAndNegativeRate() {
        assertThatThrownBy(() -> engine.applyCorrection(List.of(), bd("0.01"), List.of()))
                .isInstanceOf(CalculationException.class);
        List<Installment> base = amortization.calculateSac(bd("100.00"), bd("0.01"), 1);
        assertThatThrownBy(() -> engine.applyCorrection(base, bd("-0.01"), List.of()))
                .isInstanceOf(CalculationException.class);
    }
}
