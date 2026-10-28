package com.willembergfilho.ifinance.domain.labor;

import com.willembergfilho.ifinance.domain.labor.IrrfResult.DeductionMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayrollTaxesTest {

    private static final LaborTaxTable T2026 = LaborTaxTables.forYear(2026);
    private static final LaborTaxTable T2025 = LaborTaxTables.forYear(2025);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    // ------------------------------------------------------------------ INSS --

    @ParameterizedTest(name = "INSS 2026 on {0} = {1}")
    @CsvSource({
            "1000.00, 75.00",    // 1000 x 7.5%
            "1621.00, 121.58",   // 1621 x 7.5% = 121.575
            "2902.84, 236.94",   // 121.575 + 1281.84 x 9% (115.3656) = 236.9406
            "4354.27, 411.11",   // 236.9406 + 1451.43 x 12% (174.1716) = 411.1122
            "8475.55, 988.09",   // 411.1122 + 4121.28 x 14% (576.9792) = 988.0914 — official ceiling contribution
            "20000.00, 988.09",  // above the ceiling: capped
            "0, 0.00"
    })
    void inss2026_bracketBoundaries(String base, String expected) {
        assertThat(PayrollTaxes.inss(bd(base), T2026).total()).isEqualByComparingTo(expected);
    }

    @Test
    void inss2026_breakdownPerBracket() {
        // 3000: 121.575 (7.5%) + 115.3656 (9%) + 97.16 x 12% = 11.6592 -> 248.5998
        InssResult r = PayrollTaxes.inss(bd("3000"), T2026);

        assertThat(r.brackets()).hasSize(3);
        assertThat(r.brackets().get(1).from()).isEqualByComparingTo("1621.01");
        assertThat(r.brackets().get(1).contribution()).isEqualByComparingTo("115.37");
        assertThat(r.brackets().get(2).taxedAmount()).isEqualByComparingTo("97.16");
        assertThat(r.brackets().get(2).contribution()).isEqualByComparingTo("11.66");
        assertThat(r.total()).isEqualByComparingTo("248.60");
        assertThat(r.effectiveRate()).isEqualByComparingTo("0.0829");
    }

    @Test
    void inss2025_ceiling() {
        // 113.85 + 1275.88 x 9% (114.8292) + 1396.95 x 12% (167.634) + 3966.58 x 14% (555.3212) = 951.6344
        assertThat(PayrollTaxes.inss(bd("8157.41"), T2025).total()).isEqualByComparingTo("951.63");
    }

    @Test
    void inss_negativeBase_rejected() {
        assertThatThrownBy(() -> PayrollTaxes.inss(bd("-1"), T2026)).isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------ IRRF --

    @Test
    void irrf_receitaFederalExample_6000_simplifiedWithReduction() {
        // Receita example: 6000 - 607.20 = 5392.80; x 27.5% - 908.73 = 574.29;
        // reduction 978.62 - 0.133145 x 6000 = 179.75; tax 394.54
        IrrfResult r = PayrollTaxes.irrf(bd("6000"), ZERO, 0, ZERO, T2026);

        assertThat(r.deductionMethod()).isEqualTo(DeductionMethod.SIMPLIFIED);
        assertThat(r.base()).isEqualByComparingTo("5392.80");
        assertThat(r.taxBeforeReduction()).isEqualByComparingTo("574.29");
        assertThat(r.reduction()).isEqualByComparingTo("179.75");
        assertThat(r.tax()).isEqualByComparingTo("394.54");
    }

    @Test
    void irrf_5000_fullyExemptBy2026Reduction() {
        // INSS 501.51 < 607.20 -> simplified; base 4392.80 x 22.5% - 675.49 = 312.89 = max reduction -> 0
        IrrfResult r = PayrollTaxes.irrf(bd("5000"), bd("501.51"), 0, ZERO, T2026);

        assertThat(r.taxBeforeReduction()).isEqualByComparingTo("312.89");
        assertThat(r.reduction()).isEqualByComparingTo("312.89");
        assertThat(r.tax()).isEqualByComparingTo("0");
    }

    @Test
    void irrf_5000_withoutReductionIn2025() {
        IrrfResult r = PayrollTaxes.irrf(bd("5000"), bd("509.60"), 0, ZERO, T2025);

        assertThat(r.reduction()).isEqualByComparingTo("0");
        assertThat(r.tax()).isEqualByComparingTo("312.89");
    }

    @Test
    void irrf_6000_legalDeductionsBeatSimplified() {
        // INSS 641.51 > 607.20 -> legal; base 5358.49 x 27.5% - 908.73 = 564.85; - 179.75 = 385.10
        IrrfResult r = PayrollTaxes.irrf(bd("6000"), bd("641.51"), 0, ZERO, T2026);

        assertThat(r.deductionMethod()).isEqualTo(DeductionMethod.LEGAL);
        assertThat(r.base()).isEqualByComparingTo("5358.49");
        assertThat(r.taxBeforeReduction()).isEqualByComparingTo("564.85");
        assertThat(r.tax()).isEqualByComparingTo("385.10");
    }

    @Test
    void irrf_phaseOutEndsAt7350() {
        // 978.62 - 0.133145 x 7350 = 0.00425 -> 0.00; base 6742.80 x 27.5% - 908.73 = 945.54
        assertThat(PayrollTaxes.irrf(bd("7350"), ZERO, 0, ZERO, T2026).reduction()).isEqualByComparingTo("0");
        assertThat(PayrollTaxes.irrf(bd("7350.01"), ZERO, 0, ZERO, T2026).tax()).isEqualByComparingTo("945.54");
    }

    @Test
    void irrf_10000_twoDependents_noReduction() {
        // legal = 988.09 + 2 x 189.59 = 1367.27; base 8632.73 x 27.5% - 908.73 = 1465.27
        IrrfResult r = PayrollTaxes.irrf(bd("10000"), bd("988.09"), 2, ZERO, T2026);

        assertThat(r.dependentsDeduction()).isEqualByComparingTo("379.18");
        assertThat(r.legalDeductions()).isEqualByComparingTo("1367.27");
        assertThat(r.reduction()).isEqualByComparingTo("0");
        assertThat(r.tax()).isEqualByComparingTo("1465.27");
        assertThat(r.effectiveRate()).isEqualByComparingTo("0.1465");
    }

    @Test
    void irrf_alimonyDeductible() {
        // 2025, gross 4000, INSS 373.41: without alimony simplified base 3392.80 x 15% - 394.16 = 114.76;
        // with 800 alimony legal = 1173.41, base 2826.59 x 7.5% - 182.16 = 29.83
        assertThat(PayrollTaxes.irrf(bd("4000"), bd("373.41"), 0, ZERO, T2025).tax()).isEqualByComparingTo("114.76");
        IrrfResult r = PayrollTaxes.irrf(bd("4000"), bd("373.41"), 0, bd("800"), T2025);
        assertThat(r.deductionMethod()).isEqualTo(DeductionMethod.LEGAL);
        assertThat(r.base()).isEqualByComparingTo("2826.59");
        assertThat(r.tax()).isEqualByComparingTo("29.83");
    }

    @Test
    void irrf_exemptBracketUpperBound() {
        // base exactly 2428.80 is exempt
        assertThat(PayrollTaxes.irrf(bd("3036.00"), ZERO, 0, ZERO, T2025).tax()).isEqualByComparingTo("0");
    }

    // ---------------------------------------------------------------- tables --

    @Test
    void tables_defaultToLatestYear() {
        assertThat(LaborTaxTables.forYear(null).year()).isEqualTo(LaborTaxTables.latestYear()).isEqualTo(2026);
        assertThat(T2026.minimumWage()).isEqualByComparingTo("1621.00");
        assertThat(T2026.inssCeiling()).isEqualByComparingTo("8475.55");
    }

    @Test
    void tables_unknownYear_rejected() {
        assertThatThrownBy(() -> LaborTaxTables.forYear(2019))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2019");
    }
}
