package com.willembergfilho.ifinance.domain.credit;

import com.willembergfilho.ifinance.domain.simulation.AmortizationSystem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditCapacityCalculatorTest {

    private final CreditCapacityCalculator calculator = new CreditCapacityCalculator();

    // (1.01)^12 - 1: effective annual rate equivalent to exactly 1% a.m.
    private static final BigDecimal ANNUAL_1PCT_MONTHLY = new BigDecimal("0.126825030131969720661201");
    private static final BigDecimal INCOME = new BigDecimal("10000");
    private static final BigDecimal DEBTS = new BigDecimal("1000");

    private CreditCapacityResult calc(BigDecimal rate, BigDecimal annual, int n, AmortizationSystem system, BigDecimal desired) {
        return calculator.calculate(new CreditCapacityParameters(INCOME, DEBTS, rate, annual, n, system, desired));
    }

    @Test
    void price_maxLoan_isPresentValueOfMaxInstallment() {
        // PMT = 10000 x 0.30 - 1000 = 2000; PV = 2000 x (1 - 1.01^-12) / 0.01 = 22510.15
        CreditCapacityResult r = calc(null, ANNUAL_1PCT_MONTHLY, 12, AmortizationSystem.PRICE, null);

        assertThat(r.maxCommitmentRate()).isEqualByComparingTo("0.30");
        assertThat(r.monthlyRate()).isEqualByComparingTo("0.01");
        assertThat(r.maxInstallment()).isEqualByComparingTo("2000.00");
        assertThat(r.currentCommitmentPercent()).isEqualByComparingTo("10.00");
        assertThat(r.maxLoanAmount()).isEqualByComparingTo("22510.15");
        assertThat(r.approved()).isNull();
        assertThat(r.minimumTermMonths()).isNull();
        assertThat(r.statusMessage()).contains("parcela máxima").contains("12 meses");
    }

    @Test
    void sac_maxLoan_usesFirstInstallment() {
        // SAC first installment = PV (1/n + i) -> PV = 2000 / (1/12 + 0.01) = 21428.57
        CreditCapacityResult r = calc(null, ANNUAL_1PCT_MONTHLY, 12, AmortizationSystem.SAC, null);

        assertThat(r.maxLoanAmount()).isEqualByComparingTo("21428.57");
    }

    @Test
    void zeroRate_bothSystems_maxLoanIsInstallmentTimesTerm() {
        // i = 0: PV = 2000 x 10 = 20000 in PRICE and SAC
        assertThat(calc(null, BigDecimal.ZERO, 10, AmortizationSystem.PRICE, null).maxLoanAmount())
                .isEqualByComparingTo("20000.00");
        assertThat(calc(null, BigDecimal.ZERO, 10, AmortizationSystem.SAC, null).maxLoanAmount())
                .isEqualByComparingTo("20000.00");
    }

    @Test
    void customCommitmentRate_isApplied() {
        // 10000 x 0.40 - 1000 = 3000
        CreditCapacityResult r = calc(new BigDecimal("0.40"), BigDecimal.ZERO, 10, AmortizationSystem.PRICE, null);

        assertThat(r.maxInstallment()).isEqualByComparingTo("3000.00");
        assertThat(r.maxLoanAmount()).isEqualByComparingTo("30000.00");
    }

    @Test
    void price_desiredLoanAboveLimit_notApproved_withMinimumTerm() {
        // 30000 x 0.01 / (1 - 1.01^-12) = 2665.46 > 2000; commitment (1000 + 2665.46) / 10000 = 36.65%
        // n = 16 -> 2038.34 (too high), n = 17 -> 1927.74 (fits)
        CreditCapacityResult r = calc(null, ANNUAL_1PCT_MONTHLY, 12, AmortizationSystem.PRICE, new BigDecimal("30000"));

        assertThat(r.desiredFirstInstallment()).isEqualByComparingTo("2665.46");
        assertThat(r.resultingCommitmentPercent()).isEqualByComparingTo("36.65");
        assertThat(r.approved()).isFalse();
        assertThat(r.minimumTermMonths()).isEqualTo(17);
        assertThat(r.statusMessage()).contains("Não aprovado").contains("17 meses");
    }

    @Test
    void sac_desiredLoanAboveLimit_minimumTermUsesFirstInstallment() {
        // 30000/17 + 300 = 2064.71 > 2000; 30000/18 + 300 = 1966.67 fits
        CreditCapacityResult r = calc(null, ANNUAL_1PCT_MONTHLY, 12, AmortizationSystem.SAC, new BigDecimal("30000"));

        assertThat(r.desiredFirstInstallment()).isEqualByComparingTo("2800.00");
        assertThat(r.approved()).isFalse();
        assertThat(r.minimumTermMonths()).isEqualTo(18);
    }

    @Test
    void desiredLoanWithinLimit_isApproved_andMinimumTermMayBeShorter() {
        // i = 0: 15000 / 10 = 1500 <= 2000; shortest term: ceil(15000 / 2000) = 8
        CreditCapacityResult r = calc(null, BigDecimal.ZERO, 10, AmortizationSystem.PRICE, new BigDecimal("15000"));

        assertThat(r.desiredFirstInstallment()).isEqualByComparingTo("1500.00");
        assertThat(r.resultingCommitmentPercent()).isEqualByComparingTo("25.00");
        assertThat(r.approved()).isTrue();
        assertThat(r.minimumTermMonths()).isEqualTo(8);
        assertThat(r.statusMessage()).startsWith("Aprovado");
    }

    @Test
    void desiredLoanThatNeverFits_hasNullMinimumTerm() {
        // SAC at 1% a.m. never goes below PV x 0.01 = 5000 > 2000
        CreditCapacityResult r = calc(null, ANNUAL_1PCT_MONTHLY, 12, AmortizationSystem.SAC, new BigDecimal("500000"));

        assertThat(r.approved()).isFalse();
        assertThat(r.minimumTermMonths()).isNull();
        assertThat(r.statusMessage()).contains("nenhum prazo");
    }

    @Test
    void debtsAboveLimit_floorMaxInstallmentAtZero() {
        CreditCapacityResult r = calculator.calculate(new CreditCapacityParameters(
                INCOME, new BigDecimal("4000"), null, BigDecimal.ZERO, 12, AmortizationSystem.PRICE, new BigDecimal("1000")));

        assertThat(r.maxInstallment()).isEqualByComparingTo("0");
        assertThat(r.maxLoanAmount()).isEqualByComparingTo("0");
        assertThat(r.currentCommitmentPercent()).isEqualByComparingTo("40.00");
        assertThat(r.approved()).isFalse();
        assertThat(r.minimumTermMonths()).isNull();
        assertThat(r.statusMessage()).contains("Não há margem");
    }

    @Test
    void noMargin_desiredInstallmentRoundingToZero_isNotApproved() {
        // income 0.01 x 5% = 0.0005 -> maxInstallment 0.00; 0.001 / 1 rounds to 0.00 but must not fit
        CreditCapacityResult r = calculator.calculate(new CreditCapacityParameters(
                new BigDecimal("0.01"), BigDecimal.ZERO, new BigDecimal("0.05"), BigDecimal.ZERO, 1,
                AmortizationSystem.PRICE, new BigDecimal("0.001")));

        assertThat(r.maxInstallment()).isEqualByComparingTo("0");
        assertThat(r.approved()).isFalse();
        assertThat(r.minimumTermMonths()).isNull();
        assertThat(r.statusMessage()).doesNotContain("dívidas").contains("Não há margem");
    }

    @Test
    void statusMessage_formatsLimitWithPtBrDecimalComma() {
        CreditCapacityResult r = calc(new BigDecimal("0.333333"), BigDecimal.ZERO, 10, AmortizationSystem.PRICE,
                new BigDecimal("1000000"));

        assertThat(r.statusMessage()).contains("33,3333%").doesNotContain("33.3333");
    }

    @Test
    void unsupportedAmortizationSystem_isRejected() {
        assertThatThrownBy(() -> calc(null, BigDecimal.ZERO, 12, AmortizationSystem.AMERICAN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PRICE ou SAC");
    }

    @Test
    void commitmentRateOutOfBounds_isRejected() {
        assertThatThrownBy(() -> calc(new BigDecimal("0.80"), BigDecimal.ZERO, 12, AmortizationSystem.PRICE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
