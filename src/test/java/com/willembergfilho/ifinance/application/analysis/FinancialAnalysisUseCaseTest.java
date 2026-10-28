package com.willembergfilho.ifinance.application.analysis;

import com.willembergfilho.ifinance.application.analysis.FinancialAnalysisUseCase.AnalysisResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class FinancialAnalysisUseCaseTest {

    private final FinancialAnalysisUseCase useCase = new FinancialAnalysisUseCase();

    private static List<BigDecimal> flows(String... values) {
        return Arrays.stream(values).map(BigDecimal::new).toList();
    }

    @Test
    void positiveNpvProject() {
        // NPV @10% = -1000 + 500/1.1 + 500/1.21 + 500/1.331 = 243.43
        // IRR: 500 * a(3, r) = 1000 -> r = 23.38%; simple payback exactly 2.00
        AnalysisResult r = useCase.execute(flows("-1000", "500", "500", "500"), new BigDecimal("0.10"));

        assertThat(r.npv()).isEqualByComparingTo("243.43");
        assertThat(r.npvPositive()).isTrue();
        assertThat(r.irrPercent()).isEqualByComparingTo("23.38");
        assertThat(r.paybackAchieved()).isTrue();
        assertThat(r.simplePayback()).isEqualByComparingTo("2.00");
        // discounted: 454.55 + 413.22 = 867.77; remaining 132.23 / 375.66 = 0.352 -> 2.35
        assertThat(r.discountedPayback()).isEqualByComparingTo("2.35");
        assertThat(r.discountedPaybackPeriod()).isEqualTo(3);
    }

    @Test
    void negativeNpvWhenDiscountRateAboveIrr() {
        // NPV @10% = -1000 + 363.64 + 330.58 + 300.53 = -5.26; IRR = 9.70% < 10%
        AnalysisResult r = useCase.execute(flows("-1000", "400", "400", "400"), new BigDecimal("0.10"));

        assertThat(r.npv()).isEqualByComparingTo("-5.26");
        assertThat(r.npvPositive()).isFalse();
        assertThat(r.irrDecimal().doubleValue()).isCloseTo(0.0970103, within(1e-6));
        assertThat(r.irrPercent()).isEqualByComparingTo("9.70");
        assertThat(r.simplePayback()).isEqualByComparingTo("2.50");
        assertThat(r.discountedPayback()).isNull();
    }

    @Test
    void withoutDiscountRateNpvIsOmitted() {
        AnalysisResult r = useCase.execute(flows("-1000", "1100"), null);

        assertThat(r.npv()).isNull();
        assertThat(r.npvPositive()).isFalse();
        assertThat(r.irrPercent()).isEqualByComparingTo("10.00");
        assertThat(r.discountedPayback()).isNull();
    }

    @Test
    void irrWithoutSignChangeIsNullInsteadOfFailing() {
        AnalysisResult r = useCase.execute(flows("100", "100", "100"), new BigDecimal("0.10"));

        assertThat(r.irrPercent()).isNull();
        assertThat(r.irrDecimal()).isNull();
        assertThat(r.npvPositive()).isTrue();
    }
}
