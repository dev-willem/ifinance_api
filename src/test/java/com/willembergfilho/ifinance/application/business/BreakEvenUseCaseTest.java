package com.willembergfilho.ifinance.application.business;

import com.willembergfilho.ifinance.domain.business.BreakEvenParameters;
import com.willembergfilho.ifinance.domain.business.BreakEvenResult;
import com.willembergfilho.ifinance.domain.math.CalculationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BreakEvenUseCaseTest {

    private final BreakEvenUseCase useCase = new BreakEvenUseCase();

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void fullExample_allBreakEvensSafetyMarginAndLeverage() {
        // F=10000, P=50, CVu=30 -> MCu=20 (40%)
        // contábil 10000/20=500 un (R$25000); econômico (10000+4000)/20=700 (R$35000);
        // financeiro (10000-2000)/20=400 (R$20000)
        // Q=800: receita 40000, lucro 800*20-10000=6000, MS 300 un / R$15000 / 37,5%, GAO 16000/6000=2,666667
        BreakEvenResult r = useCase.execute(new BreakEvenParameters(bd("10000"), bd("50"), bd("30"),
                bd("2000"), bd("4000"), bd("800")));

        assertThat(r.unitContributionMargin()).isEqualByComparingTo("20");
        assertThat(r.contributionMarginRatio()).isEqualByComparingTo("0.4");
        assertThat(r.accountingBreakEvenUnits()).isEqualByComparingTo("500");
        assertThat(r.accountingBreakEvenRevenue()).isEqualByComparingTo("25000");
        assertThat(r.economicBreakEvenUnits()).isEqualByComparingTo("700");
        assertThat(r.economicBreakEvenRevenue()).isEqualByComparingTo("35000");
        assertThat(r.financialBreakEvenUnits()).isEqualByComparingTo("400");
        assertThat(r.financialBreakEvenRevenue()).isEqualByComparingTo("20000");
        assertThat(r.currentRevenue()).isEqualByComparingTo("40000");
        assertThat(r.operatingProfit()).isEqualByComparingTo("6000");
        assertThat(r.safetyMarginUnits()).isEqualByComparingTo("300");
        assertThat(r.safetyMarginRevenue()).isEqualByComparingTo("15000");
        assertThat(r.safetyMarginRatio()).isEqualByComparingTo("0.375");
        assertThat(r.operatingLeverageDegree()).isEqualByComparingTo("2.666667");
    }

    @Test
    void fractionalBreakEven_unitsRoundUpAndExactIsKept() {
        // 1000 / (10-7) = 333,3333 -> 334 un; revenue 333,333.. * 10 = 3333,33
        BreakEvenResult r = useCase.execute(new BreakEvenParameters(bd("1000"), bd("10"), bd("7"),
                null, null, null));

        assertThat(r.accountingBreakEvenUnitsExact()).isEqualByComparingTo("333.3333");
        assertThat(r.accountingBreakEvenUnits()).isEqualByComparingTo("334");
        assertThat(r.accountingBreakEvenRevenue()).isEqualByComparingTo("3333.33");
        assertThat(r.financialBreakEvenUnits()).isEqualByComparingTo("334");
        assertThat(r.economicBreakEvenUnits()).isNull();
        assertThat(r.safetyMarginRatio()).isNull();
        assertThat(r.operatingLeverageDegree()).isNull();
    }

    @Test
    void exactlyAtBreakEven_leverageIsUndefined() {
        // Q=500 is the break-even: profit 0 -> GAO would divide by zero
        BreakEvenResult r = useCase.execute(new BreakEvenParameters(bd("10000"), bd("50"), bd("30"),
                null, null, bd("500")));

        assertThat(r.operatingProfit()).isEqualByComparingTo("0");
        assertThat(r.safetyMarginRatio()).isEqualByComparingTo("0");
        assertThat(r.operatingLeverageDegree()).isNull();
    }

    @Test
    void belowBreakEven_negativeSafetyMargin() {
        // Q=400: profit 400*20-10000=-2000; MS -100 un (-25%)
        BreakEvenResult r = useCase.execute(new BreakEvenParameters(bd("10000"), bd("50"), bd("30"),
                null, null, bd("400")));

        assertThat(r.operatingProfit()).isEqualByComparingTo("-2000");
        assertThat(r.safetyMarginUnits()).isEqualByComparingTo("-100");
        assertThat(r.safetyMarginRatio()).isEqualByComparingTo("-0.25");
        // GAO = 8000 / -2000 = -4
        assertThat(r.operatingLeverageDegree()).isEqualByComparingTo("-4");
    }

    @Test
    void zeroFixedCosts_breakEvenIsZero() {
        BreakEvenResult r = useCase.execute(new BreakEvenParameters(bd("0"), bd("10"), bd("4"),
                null, null, bd("10")));

        assertThat(r.accountingBreakEvenUnits()).isEqualByComparingTo("0");
        assertThat(r.operatingLeverageDegree()).isEqualByComparingTo("1");
    }

    @Test
    void priceNotAboveVariableCost_isCalculationError() {
        assertThatThrownBy(() -> useCase.execute(new BreakEvenParameters(bd("1000"), bd("10"), bd("10"),
                null, null, null))).isInstanceOf(CalculationException.class)
                .hasMessageContaining("custo variável");
        assertThatThrownBy(() -> useCase.execute(new BreakEvenParameters(bd("1000"), bd("10"), bd("12"),
                null, null, null))).isInstanceOf(CalculationException.class);
    }

    @Test
    void invalidInputs_areRejected() {
        assertThatThrownBy(() -> new BreakEvenParameters(bd("1000"), bd("0"), bd("0"), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BreakEvenParameters(bd("1000"), bd("10"), bd("5"), bd("1001"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BreakEvenParameters(bd("1000"), bd("10"), bd("5"), null, null, bd("0")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
