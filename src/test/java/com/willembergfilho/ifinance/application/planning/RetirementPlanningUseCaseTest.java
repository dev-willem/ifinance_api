package com.willembergfilho.ifinance.application.planning;

import com.willembergfilho.ifinance.domain.planning.RetirementParameters;
import com.willembergfilho.ifinance.domain.planning.RetirementResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class RetirementPlanningUseCaseTest {

    private final RetirementPlanningUseCase useCase = new RetirementPlanningUseCase();

    private static RetirementParameters params(String expenses, String current, String monthly, String annualReturn) {
        return new RetirementParameters(new BigDecimal(expenses), new BigDecimal(current), new BigDecimal(monthly),
                new BigDecimal(annualReturn), new BigDecimal("0.04"));
    }

    @Test
    void targetFollowsFourPercentRule() {
        // 4000 * 12 = 48000 a.a.; 48000 / 0.04 = 1,200,000
        RetirementResult r = useCase.execute(params("4000", "0", "1000", "0.08"));

        assertThat(r.annualExpenses()).isEqualByComparingTo("48000.00");
        assertThat(r.targetAmount()).isEqualByComparingTo("1200000.00");
    }

    @Test
    void monthsToFireWithCompoundReturn() {
        // r = 1.08^(1/12) - 1 = 0.0064340301
        // n = ln((1.2M*r + 5000) / (100k*r + 5000)) / ln(1+r) = 126.73 -> 127 months (10 years)
        // FV(120) = 100000*(1+r)^120 + 5000*((1+r)^120 - 1)/r = 1,116,513.87
        RetirementResult r = useCase.execute(params("4000", "100000", "5000", "0.08"));

        assertThat(r.monthlyRate()).isEqualByComparingTo("0.0064340301");
        assertThat(r.monthsToFire()).isEqualTo(127);
        assertThat(r.yearsToFire()).isEqualTo(10);
        assertThat(r.remainingMonths().doubleValue()).isCloseTo(126.727, within(0.001));
        assertThat(r.alreadyFire()).isFalse();
        assertThat(r.projectedAmountAt10Years().doubleValue()).isCloseTo(1116513.87, within(0.01));
        assertThat(r.projectedAmountAt20Years()).isGreaterThan(r.targetAmount());
    }

    @Test
    void zeroReturnIsLinear() {
        // (1,200,000 - 200,000) / 10,000 = 100 months; projections 200k + 10k*120 and 200k + 10k*240
        RetirementResult r = useCase.execute(params("4000", "200000", "10000", "0"));

        assertThat(r.monthsToFire()).isEqualTo(100);
        assertThat(r.yearsToFire()).isEqualTo(8);
        assertThat(r.projectedAmountAt10Years()).isEqualByComparingTo("1400000.00");
        assertThat(r.projectedAmountAt20Years()).isEqualByComparingTo("2600000.00");
    }

    @Test
    void alreadyFinanciallyIndependent() {
        RetirementResult r = useCase.execute(params("4000", "1300000", "0", "0.05"));

        assertThat(r.alreadyFire()).isTrue();
        assertThat(r.monthsToFire()).isNull();
        assertThat(r.yearsToFire()).isNull();
    }

    @Test
    void neverReachedWithoutSavingsOrReturn() {
        RetirementResult r = useCase.execute(params("4000", "1000", "0", "0"));

        assertThat(r.alreadyFire()).isFalse();
        assertThat(r.monthsToFire()).isNull();
        assertThat(r.projectedAmountAt20Years()).isEqualByComparingTo("1000.00");
    }
}
