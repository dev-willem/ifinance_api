package com.willembergfilho.ifinance.application.business;

import com.willembergfilho.ifinance.domain.business.MarkupParameters;
import com.willembergfilho.ifinance.domain.business.MarkupParameters.NamedRate;
import com.willembergfilho.ifinance.domain.business.MarkupResult;
import com.willembergfilho.ifinance.domain.business.MarkupResult.PriceComponent;
import com.willembergfilho.ifinance.domain.math.CalculationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkupUseCaseTest {

    private final MarkupUseCase useCase = new MarkupUseCase();

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static BigDecimal amount(MarkupResult r, String key) {
        return r.composition().stream().filter(c -> c.key().equals(key)).findFirst().orElseThrow().amount();
    }

    @Test
    void divisorMethod_priceAndComposition() {
        // 10% + 5% + 3% + 12% + 10% = 40% -> divisor 0,6, multiplier 1,666667, price 60/0,6 = 100
        // at R$ 90: profit = 90*(1-0,30) - 60 = 3 -> margin 3/90 = 3,3333%
        MarkupResult r = useCase.execute(new MarkupParameters(bd("60"), bd("0.10"), bd("0.05"), bd("0.03"),
                bd("0.12"), bd("0.10"), null, bd("90")));

        assertThat(r.totalRate()).isEqualByComparingTo("0.4");
        assertThat(r.markupDivisor()).isEqualByComparingTo("0.6");
        assertThat(r.markupMultiplier()).isEqualByComparingTo("1.666667");
        assertThat(r.suggestedPrice()).isEqualByComparingTo("100");
        assertThat(r.composition()).extracting(PriceComponent::key)
                .containsExactly("COST", "SALES_TAX", "COMMISSION", "CARD_FEE", "FIXED_EXPENSE", "PROFIT");
        assertThat(amount(r, "COST")).isEqualByComparingTo("60");
        assertThat(amount(r, "SALES_TAX")).isEqualByComparingTo("10");
        assertThat(amount(r, "FIXED_EXPENSE")).isEqualByComparingTo("12");
        assertThat(amount(r, "PROFIT")).isEqualByComparingTo("10");
        assertThat(r.actualProfitAmount()).isEqualByComparingTo("3");
        assertThat(r.actualProfitMargin()).isEqualByComparingTo("0.033333");
    }

    @Test
    void rounding_compositionAddsUpExactlyToPrice() {
        // divisor 1-0,18-0,15 = 0,67; 10/0,67 = 14,925.. -> 14,93; tax 14,93*0,18 = 2,6874 -> 2,69;
        // profit = 14,93 - 10 - 2,69 = 2,24
        MarkupResult r = useCase.execute(new MarkupParameters(bd("10"), bd("0.18"), null, null, null,
                bd("0.15"), null, null));

        assertThat(r.suggestedPrice()).isEqualByComparingTo("14.93");
        assertThat(amount(r, "SALES_TAX")).isEqualByComparingTo("2.69");
        assertThat(amount(r, "PROFIT")).isEqualByComparingTo("2.24");
        BigDecimal sum = r.composition().stream().map(PriceComponent::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(r.suggestedPrice());
        assertThat(r.composition()).extracting(PriceComponent::key).containsExactly("COST", "SALES_TAX", "PROFIT");
        assertThat(r.actualProfitMargin()).isNull();
    }

    @Test
    void extraRates_areNamedComponents() {
        // 10% + Frete 5% + 5% profit = 20% -> 80/0,8 = 100
        MarkupResult r = useCase.execute(new MarkupParameters(bd("80"), bd("0.10"), null, null, null,
                bd("0.05"), List.of(new NamedRate(" Frete ", bd("0.05"))), null));

        assertThat(r.suggestedPrice()).isEqualByComparingTo("100");
        PriceComponent extra = r.composition().get(2);
        assertThat(extra.key()).isEqualTo("EXTRA");
        assertThat(extra.label()).isEqualTo("Frete");
        assertThat(extra.amount()).isEqualByComparingTo("5");
    }

    @Test
    void currentPriceBelowCost_negativeMargin() {
        // 50*(1-0,10) - 60 = -15 -> -30%
        MarkupResult r = useCase.execute(new MarkupParameters(bd("60"), bd("0.10"), null, null, null,
                bd("0.10"), null, bd("50")));

        assertThat(r.actualProfitAmount()).isEqualByComparingTo("-15");
        assertThat(r.actualProfitMargin()).isEqualByComparingTo("-0.3");
    }

    @Test
    void ratesSummingTo100Percent_isCalculationError() {
        assertThatThrownBy(() -> useCase.execute(new MarkupParameters(bd("10"), bd("0.5"), null, null, null,
                bd("0.5"), null, null)))
                .isInstanceOf(CalculationException.class)
                .hasMessageContaining("100,00%");
        assertThatThrownBy(() -> useCase.execute(new MarkupParameters(bd("10"), bd("0.6"), null, null, null,
                bd("0.3"), List.of(new NamedRate("X", bd("0.2"))), null)))
                .isInstanceOf(CalculationException.class)
                .hasMessageContaining("110,00%");
    }

    @Test
    void zeroProfit_roundingCentGoesToLargestExpense_noProfitLine() {
        // divisor 1-3*0,007 = 0,979; 1,01/0,979 = 1,0317 -> 1,03; each expense 1,03*0,007 = 0,0072 -> 0,01;
        // 1,01 + 0,03 = 1,04 -> remainder -0,01 lands on the first largest expense (SALES_TAX 0,00)
        MarkupResult r = useCase.execute(new MarkupParameters(bd("1.01"), bd("0.007"), bd("0.007"), bd("0.007"),
                null, null, null, null));

        assertThat(r.suggestedPrice()).isEqualByComparingTo("1.03");
        assertThat(r.composition()).extracting(PriceComponent::key)
                .containsExactly("COST", "SALES_TAX", "COMMISSION", "CARD_FEE");
        assertThat(amount(r, "SALES_TAX")).isEqualByComparingTo("0");
        BigDecimal sum = r.composition().stream().map(PriceComponent::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(r.suggestedPrice());

        // 90/0,9 = 100 exactly: no remainder, still no PROFIT line at 0% profit
        MarkupResult exact = useCase.execute(new MarkupParameters(bd("90"), bd("0.10"), null, null, null,
                null, null, null));
        assertThat(exact.composition()).extracting(PriceComponent::key).containsExactly("COST", "SALES_TAX");
    }

    @Test
    void divisorBelowOnePercent_isCalculationError() {
        // 99,5% leaves 0,5% for the cost (multiplier 200x); exactly 99% (100x) is still accepted
        assertThatThrownBy(() -> useCase.execute(new MarkupParameters(bd("10"), bd("0.995"), null, null, null,
                null, null, null)))
                .isInstanceOf(CalculationException.class)
                .hasMessageContaining("inferior a 1%");
        assertThat(useCase.execute(new MarkupParameters(bd("10"), bd("0.99"), null, null, null, null, null, null))
                .suggestedPrice()).isEqualByComparingTo("1000");
    }

    @Test
    void invalidInputs_areRejected() {
        assertThatThrownBy(() -> new MarkupParameters(bd("0"), null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MarkupParameters(bd("1"), bd("-0.1"), null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NamedRate(" ", bd("0.1"))).isInstanceOf(IllegalArgumentException.class);
    }
}
