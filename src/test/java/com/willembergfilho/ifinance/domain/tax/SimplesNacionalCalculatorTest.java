package com.willembergfilho.ifinance.domain.tax;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.willembergfilho.ifinance.domain.tax.SimplesAnnex.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimplesNacionalCalculatorTest {

    private final SimplesNacionalCalculator calculator = new SimplesNacionalCalculator();

    private SimplesResult calc(String rbt12, String revenue, SimplesAnnex annex, String payroll12) {
        return calculator.calculate(new SimplesParameters(new BigDecimal(rbt12), new BigDecimal(revenue), annex,
                payroll12 == null ? null : new BigDecimal(payroll12)));
    }

    private static BigDecimal amount(SimplesResult r, TaxType tax) {
        return r.shares().stream().filter(s -> s.tax() == tax).findFirst().orElseThrow().amount();
    }

    @Test
    void annexI_faixa1_effectiveRateEqualsNominal() {
        // RBT12 150.000 → 1ª faixa, PD 0 → efetiva = 4%; DAS = 10.000 × 4% = 400,00
        SimplesResult r = calc("150000", "10000", I, null);

        assertThat(r.eligible()).isTrue();
        assertThat(r.bracket()).isEqualTo(1);
        assertThat(r.effectiveRate()).isEqualByComparingTo(r.nominalRate()).isEqualByComparingTo("0.04");
        assertThat(r.dasAmount()).isEqualByComparingTo("400.00");
        assertThat(amount(r, TaxType.ICMS)).isEqualByComparingTo("136.00"); // 400 × 34%
        assertThat(amount(r, TaxType.CPP)).isEqualByComparingTo("166.00");  // 400 × 41,5%
        assertThat(amount(r, TaxType.COFINS)).isEqualByComparingTo("50.96"); // 400 × 12,74%
    }

    @Test
    void annexIII_faixa2() {
        // (300.000 × 11,2% − 9.360) / 300.000 = 24.240 / 300.000 = 8,08%; DAS = 25.000 × 8,08% = 2.020,00
        SimplesResult r = calc("300000", "25000", III, null);

        assertThat(r.bracket()).isEqualTo(2);
        assertThat(r.effectiveRate()).isEqualByComparingTo("0.0808");
        assertThat(r.dasAmount()).isEqualByComparingTo("2020.00");
        assertThat(amount(r, TaxType.ISS)).isEqualByComparingTo("646.40"); // 2.020 × 32%
    }

    @Test
    void annexII_faixa4_includesIpi() {
        // (1.000.000 × 11,2% − 22.500) / 1.000.000 = 8,95%; DAS = 100.000 × 8,95% = 8.950,00
        SimplesResult r = calc("1000000", "100000", II, null);

        assertThat(r.bracket()).isEqualTo(4);
        assertThat(r.dasAmount()).isEqualByComparingTo("8950.00");
        assertThat(amount(r, TaxType.IPI)).isEqualByComparingTo("671.25");  // 8.950 × 7,5%
        assertThat(amount(r, TaxType.ICMS)).isEqualByComparingTo("2864.00"); // 8.950 × 32%
    }

    @Test
    void annexIII_faixa5_capsIssAtFivePercent() {
        // (3.000.000 × 21% − 125.640) / 3.000.000 = 16,812% > 14,92537% → ISS fixo em 5%
        // ISS = 250.000 × 5% = 12.500; federais = 250.000 × 11,812% = 29.530 repartidos 6,02/5,26/19,28/4,18/65,26
        SimplesResult r = calc("3000000", "250000", III, null);

        assertThat(r.effectiveRate()).isEqualByComparingTo("0.16812");
        assertThat(r.dasAmount()).isEqualByComparingTo("42030.00");
        assertThat(amount(r, TaxType.ISS)).isEqualByComparingTo("12500.00");
        assertThat(amount(r, TaxType.IRPJ)).isEqualByComparingTo("1777.71");
        assertThat(amount(r, TaxType.CSLL)).isEqualByComparingTo("1553.28");
        assertThat(amount(r, TaxType.COFINS)).isEqualByComparingTo("5693.38");
        assertThat(amount(r, TaxType.PIS_PASEP)).isEqualByComparingTo("1234.35");
        assertThat(amount(r, TaxType.CPP)).isEqualByComparingTo("19271.28");
    }

    @Test
    void annexIV_hasNoCppInDas() {
        // (500.000 × 10,2% − 12.420) / 500.000 = 7,716%; DAS = 40.000 × 7,716% = 3.086,40
        SimplesResult r = calc("500000", "40000", IV, null);

        assertThat(r.dasAmount()).isEqualByComparingTo("3086.40");
        assertThat(r.shares()).noneMatch(s -> s.tax() == TaxType.CPP);
        assertThat(r.notes()).anyMatch(n -> n.contains("CPP"));
    }

    @Test
    void annexI_faixa6_aboveSublimit_excludesIcms() {
        // (4.000.000 × 19% − 378.000) / 4.000.000 = 9,55%; DAS = 400.000 × 9,55% = 38.200,00
        SimplesResult r = calc("4000000", "400000", I, null);

        assertThat(r.bracket()).isEqualTo(6);
        assertThat(r.aboveSublimit()).isTrue();
        assertThat(r.dasAmount()).isEqualByComparingTo("38200.00");
        assertThat(r.shares()).noneMatch(s -> s.tax() == TaxType.ICMS);
        assertThat(amount(r, TaxType.IRPJ)).isEqualByComparingTo("5157.00"); // 38.200 × 13,5%
    }

    @Test
    void fatorR_atLeast28Percent_movesAnnexVToAnnexIII() {
        // 168.000 / 600.000 = 28% → Anexo III 3ª faixa: (600.000 × 13,5% − 17.640) / 600.000 = 10,56%
        SimplesResult r = calc("600000", "50000", V, "168000");

        assertThat(r.requestedAnnex()).isEqualTo(V);
        assertThat(r.appliedAnnex()).isEqualTo(III);
        assertThat(r.fatorR()).isEqualByComparingTo("0.28");
        assertThat(r.effectiveRate()).isEqualByComparingTo("0.1056");
        assertThat(r.dasAmount()).isEqualByComparingTo("5280.00");
    }

    @Test
    void fatorR_below28Percent_movesAnnexIIIToAnnexV() {
        // 150.000 / 600.000 = 25% → Anexo V 3ª faixa: (600.000 × 19,5% − 9.900) / 600.000 = 17,85%
        SimplesResult r = calc("600000", "50000", III, "150000");

        assertThat(r.appliedAnnex()).isEqualTo(V);
        assertThat(r.effectiveRate()).isEqualByComparingTo("0.1785");
    }

    @Test
    void fatorR_isIgnoredOutsideAnnexesIIIAndV() {
        SimplesResult r = calc("600000", "50000", I, "10");

        assertThat(r.appliedAnnex()).isEqualTo(I);
        assertThat(r.fatorR()).isNull();
    }

    @Test
    void rbt12AtLimit_isEligible_andAboveLimit_isNot() {
        assertThat(calc("4800000", "400000", III, null).eligible()).isTrue();

        SimplesResult r = calc("4800000.01", "400000", III, null);
        assertThat(r.eligible()).isFalse();
        assertThat(r.dasAmount()).isNull();
        assertThat(r.shares()).isEmpty();
    }

    @Test
    void sharesAlwaysAddUpToDas() {
        for (SimplesAnnex annex : SimplesAnnex.values()) {
            for (String rbt12 : new String[]{"1", "179999.99", "250000", "1234567.89", "3599999.99", "4799999.99"}) {
                SimplesResult r = calc(rbt12, "123456.78", annex, null);
                BigDecimal sum = r.shares().stream().map(TaxShare::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
                assertThat(sum).as("%s %s", annex, rbt12).isEqualByComparingTo(r.dasAmount());
            }
        }
    }

    @Test
    void legalTablesRepartitionSumsTo100Percent() {
        for (SimplesAnnex annex : SimplesAnnex.values()) {
            for (SimplesAnnex.Bracket b : annex.brackets()) {
                BigDecimal sum = b.shares().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                assertThat(sum).as("%s faixa %d", annex, b.number()).isEqualByComparingTo("1");
            }
            if (annex.issCapRedistribution() != null) {
                assertThat(annex.issCapRedistribution().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                        .isEqualByComparingTo("1");
            }
        }
    }

    @Test
    void zeroMonthlyRevenue_givesZeroDas() {
        assertThat(calc("100000", "0", V, null).dasAmount()).isEqualByComparingTo("0");
    }

    @Test
    void invalidInputs_throw() {
        assertThatThrownBy(() -> calc("0", "1000", I, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc("1000", "-1", I, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc("1000", "1", III, "-1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc("1000", "1", null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
