package com.willembergfilho.ifinance.application.business;

import com.willembergfilho.ifinance.domain.business.IncomeStatementParameters;
import com.willembergfilho.ifinance.domain.business.IncomeStatementResult;
import com.willembergfilho.ifinance.domain.business.IncomeStatementResult.ProjectionMonth;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncomeStatementUseCaseTest {

    private final IncomeStatementUseCase useCase = new IncomeStatementUseCase();

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static IncomeStatementParameters params(String admin, Integer months, String growth) {
        return new IncomeStatementParameters(bd("100000"), bd("0.10"), bd("40000"), bd("5000"), bd(admin),
                bd("2000"), bd("3000"), bd("1000"), bd("6000"), null, months, growth == null ? null : bd(growth));
    }

    @Test
    void profitableStatement_allLinesAndMargins() {
        // 100000 - 10% = 90000 net; -40000 = 50000 GP; -(5000+15000+2000) = 28000 EBITDA; -3000 = 25000 EBIT;
        // +1000-6000 = 20000 LAIR; 34% = 6800 tax; 13200 net income.
        IncomeStatementResult r = useCase.execute(params("15000", null, null));

        assertThat(r.salesDeductions()).isEqualByComparingTo("10000");
        assertThat(r.netRevenue()).isEqualByComparingTo("90000");
        assertThat(r.grossProfit()).isEqualByComparingTo("50000");
        assertThat(r.operatingExpenses()).isEqualByComparingTo("22000");
        assertThat(r.ebitda()).isEqualByComparingTo("28000");
        assertThat(r.ebit()).isEqualByComparingTo("25000");
        assertThat(r.financialResult()).isEqualByComparingTo("-5000");
        assertThat(r.earningsBeforeTax()).isEqualByComparingTo("20000");
        assertThat(r.incomeTax()).isEqualByComparingTo("6800");
        assertThat(r.netIncome()).isEqualByComparingTo("13200");
        // margins over net revenue 90000
        assertThat(r.grossMargin()).isEqualByComparingTo("0.555556");
        assertThat(r.ebitdaMargin()).isEqualByComparingTo("0.311111");
        assertThat(r.operatingMargin()).isEqualByComparingTo("0.277778");
        assertThat(r.netMargin()).isEqualByComparingTo("0.146667");
        assertThat(r.projection()).isNull();
    }

    @Test
    void lossMakingLair_paysNoIncomeTax() {
        // admin 45000 -> opex 52000, EBITDA -2000, EBIT -5000, LAIR -10000, tax 0, net -10000
        IncomeStatementResult r = useCase.execute(params("45000", null, null));

        assertThat(r.earningsBeforeTax()).isEqualByComparingTo("-10000");
        assertThat(r.incomeTax()).isEqualByComparingTo("0");
        assertThat(r.netIncome()).isEqualByComparingTo("-10000");
        assertThat(r.netMargin()).isEqualByComparingTo("-0.111111");
    }

    @Test
    void zeroNetRevenue_marginsAreNullInsteadOfDividingByZero() {
        IncomeStatementResult r = useCase.execute(new IncomeStatementParameters(bd("0"), null, bd("0"), null,
                bd("1000"), null, null, null, null, null, null, null));

        assertThat(r.netIncome()).isEqualByComparingTo("-1000");
        assertThat(r.grossMargin()).isNull();
        assertThat(r.ebitdaMargin()).isNull();
        assertThat(r.operatingMargin()).isNull();
        assertThat(r.netMargin()).isNull();
    }

    @Test
    void customTaxRate_isApplied() {
        // LAIR 20000 * 15% = 3000
        IncomeStatementResult r = useCase.execute(new IncomeStatementParameters(bd("100000"), bd("0.10"),
                bd("40000"), bd("5000"), bd("15000"), bd("2000"), bd("3000"), bd("1000"), bd("6000"),
                bd("0.15"), null, null));

        assertThat(r.incomeTax()).isEqualByComparingTo("3000");
        assertThat(r.netIncome()).isEqualByComparingTo("17000");
    }

    @Test
    void projection_scalesVariableLinesAndKeepsFixedConstant() {
        // No other/D&A/financial lines. Growth 10%/month:
        // m1: gross 100000, net 90000, GP 50000, opex 20000, EBITDA 30000, tax 10200, NI 19800
        // m2: gross 110000, net 99000, GP 55000, opex 5500+15000=20500, EBITDA 34500, tax 11730, NI 22770
        // m3: gross 121000, net 108900, GP 60500, opex 6050+15000=21050, EBITDA 39450, tax 13413, NI 26037
        IncomeStatementResult r = useCase.execute(new IncomeStatementParameters(bd("100000"), bd("0.10"),
                bd("40000"), bd("5000"), bd("15000"), null, null, null, null, null, 3, bd("0.10")));

        List<ProjectionMonth> p = r.projection();
        assertThat(p).hasSize(3);
        assertThat(p.get(0).netIncome()).isEqualByComparingTo("19800");
        assertThat(p.get(1).grossRevenue()).isEqualByComparingTo("110000");
        assertThat(p.get(1).ebitda()).isEqualByComparingTo("34500");
        assertThat(p.get(1).netIncome()).isEqualByComparingTo("22770");
        assertThat(p.get(2).grossRevenue()).isEqualByComparingTo("121000");
        assertThat(p.get(2).ebitda()).isEqualByComparingTo("39450");
        assertThat(p.get(2).incomeTax()).isEqualByComparingTo("13413");
        assertThat(p.get(2).cumulativeNetIncome()).isEqualByComparingTo("68607");
        assertThat(r.netIncome()).isEqualByComparingTo(p.get(0).netIncome());
    }

    @Test
    void projection_monthWithLossHasNoTax() {
        // Revenue shrinks 50%/month: m2 gross 50000, net 45000, GP 25000, opex 2500+15000, EBITDA 7500;
        // m3 gross 25000, net 22500, GP 12500, opex 1250+15000=16250, EBITDA -3750 -> no tax
        IncomeStatementResult r = useCase.execute(new IncomeStatementParameters(bd("100000"), bd("0.10"),
                bd("40000"), bd("5000"), bd("15000"), null, null, null, null, null, 3, bd("-0.5")));

        ProjectionMonth m3 = r.projection().get(2);
        assertThat(m3.earningsBeforeTax()).isEqualByComparingTo("-3750");
        assertThat(m3.incomeTax()).isEqualByComparingTo("0");
        assertThat(m3.netIncome()).isEqualByComparingTo("-3750");
    }

    @Test
    void invalidInputs_areRejected() {
        assertThatThrownBy(() -> new IncomeStatementParameters(bd("-1"), null, bd("0"), null, null, null,
                null, null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IncomeStatementParameters(bd("1"), bd("1.01"), bd("0"), null, null, null,
                null, null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> params("1", 61, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> params("1", 3, "-1")).isInstanceOf(IllegalArgumentException.class);
    }
}
