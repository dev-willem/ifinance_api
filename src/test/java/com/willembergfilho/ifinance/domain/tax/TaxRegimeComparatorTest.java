package com.willembergfilho.ifinance.domain.tax;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.willembergfilho.ifinance.domain.tax.BusinessActivity.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxRegimeComparatorTest {

    private final TaxRegimeComparator comparator = new TaxRegimeComparator();

    private TaxComparisonResult compare(String revenue, BusinessActivity activity, String payroll,
                                        String deductible, String purchases, String salesTaxRate) {
        return comparator.compare(new TaxComparisonParameters(new BigDecimal(revenue), activity,
                new BigDecimal(payroll), null, new BigDecimal(deductible), new BigDecimal(purchases),
                new BigDecimal(salesTaxRate)));
    }

    private static RegimeResult regime(TaxComparisonResult r, TaxRegime regime) {
        return r.regimes().stream().filter(x -> x.regime() == regime).findFirst().orElseThrow();
    }

    private static BigDecimal tax(RegimeResult r, TaxType tax) {
        return r.breakdown().stream().filter(s -> s.tax() == tax).findFirst().orElseThrow().amount();
    }

    @Test
    void smallCommerce_simplesIsCheapest() {
        TaxComparisonResult r = compare("600000", COMMERCE, "60000", "300000", "0", "0.12");

        // Simples Anexo I 3ª faixa: (600.000 × 9,5% − 13.860) / 600.000 = 7,19% → 43.140,00
        RegimeResult simples = regime(r, TaxRegime.SIMPLES_NACIONAL);
        assertThat(simples.total()).isEqualByComparingTo("43140.00");
        assertThat(simples.netProfit()).isEqualByComparingTo("196860.00"); // 600.000 − 300.000 − 60.000 − 43.140

        // Presumido: IRPJ 600.000×8%×15% = 7.200; CSLL 600.000×12%×9% = 6.480; ICMS 72.000;
        // PIS/Cofins on 528.000 (ICMS excluded) = 3.432 + 15.840; CPP 60.000 × 27,8% = 16.680
        RegimeResult presumido = regime(r, TaxRegime.LUCRO_PRESUMIDO);
        assertThat(tax(presumido, TaxType.PIS_PASEP)).isEqualByComparingTo("3432.00");
        assertThat(tax(presumido, TaxType.CPP)).isEqualByComparingTo("16680.00");
        assertThat(presumido.total()).isEqualByComparingTo("121632.00");

        // Real: profit = 600.000 − 72.000 − 48.840 − 300.000 − 60.000 − 16.680 = 102.480
        // IRPJ 15.372; CSLL 9.223,20; total = 72.000 + 8.712 + 40.128 + 16.680 + 15.372 + 9.223,20
        RegimeResult real = regime(r, TaxRegime.LUCRO_REAL);
        assertThat(tax(real, TaxType.IRPJ)).isEqualByComparingTo("15372.00");
        assertThat(real.total()).isEqualByComparingTo("162115.20");

        assertThat(r.cheapest()).isEqualTo(TaxRegime.SIMPLES_NACIONAL);
        assertThat(r.disclaimer()).contains("LC 214/2025");
    }

    @Test
    void presumido_irpjSurtaxStartsAbove240kOfPresumedProfit() {
        // 750.000 × 32% = 240.000 → only 15% = 36.000
        assertThat(tax(regime(compare("750000", SERVICES, "0", "0", "0", "0"), TaxRegime.LUCRO_PRESUMIDO),
                TaxType.IRPJ)).isEqualByComparingTo("36000.00");
        // 1.000.000 × 32% = 320.000 → 48.000 + 10% × 80.000 = 56.000
        assertThat(tax(regime(compare("1000000", SERVICES, "0", "0", "0", "0"), TaxRegime.LUCRO_PRESUMIDO),
                TaxType.IRPJ)).isEqualByComparingTo("56000.00");
    }

    @Test
    void highRevenueLowMargin_lucroRealIsCheapest() {
        TaxComparisonResult r = compare("10000000", COMMERCE, "0", "9500000", "8000000", "0");

        assertThat(regime(r, TaxRegime.SIMPLES_NACIONAL).eligible()).isFalse();

        // Presumido with LC 224/2025: IRPJ base = 800.000 + 5.000.000×8%×10% = 840.000 → 126.000 + 60.000
        // CSLL base = 1.200.000 + 60.000 = 1.260.000 → 113.400; PIS 65.000; Cofins 300.000
        RegimeResult presumido = regime(r, TaxRegime.LUCRO_PRESUMIDO);
        assertThat(tax(presumido, TaxType.IRPJ)).isEqualByComparingTo("186000.00");
        assertThat(tax(presumido, TaxType.CSLL)).isEqualByComparingTo("113400.00");
        assertThat(presumido.total()).isEqualByComparingTo("664400.00");

        // Real: PIS 165.000 − 132.000; Cofins 760.000 − 608.000;
        // profit = 10.000.000 − 925.000 + 740.000 − 9.500.000 = 315.000 → IRPJ 47.250 + 7.500; CSLL 28.350
        RegimeResult real = regime(r, TaxRegime.LUCRO_REAL);
        assertThat(tax(real, TaxType.PIS_PASEP)).isEqualByComparingTo("33000.00");
        assertThat(tax(real, TaxType.COFINS)).isEqualByComparingTo("152000.00");
        assertThat(tax(real, TaxType.IRPJ)).isEqualByComparingTo("54750.00");
        assertThat(real.total()).isEqualByComparingTo("268100.00");

        assertThat(r.cheapest()).isEqualTo(TaxRegime.LUCRO_REAL);
    }

    @Test
    void lucroReal_lossYear_hasNoIrpjOrCsll() {
        RegimeResult real = regime(compare("500000", SERVICES, "100000", "450000", "0", "0.05"), TaxRegime.LUCRO_REAL);

        assertThat(tax(real, TaxType.IRPJ)).isEqualByComparingTo("0");
        assertThat(tax(real, TaxType.CSLL)).isEqualByComparingTo("0");
        assertThat(real.netProfit()).isNegative();
        assertThat(real.notes()).anyMatch(n -> n.contains("Prejuízo"));
    }

    @Test
    void annexIV_paysCppOutsideDas() {
        // DAS: (360.000 × 9% − 8.100) / 360.000 = 6,75% → 24.300; CPP 100.000 × 22% = 22.000
        RegimeResult simples = regime(compare("360000", SERVICES_ANNEX_IV, "100000", "0", "0", "0.05"),
                TaxRegime.SIMPLES_NACIONAL);

        assertThat(tax(simples, TaxType.CPP)).isEqualByComparingTo("22000.00");
        assertThat(simples.total()).isEqualByComparingTo("46300.00");
        assertThat(simples.notes()).anyMatch(n -> n.contains("22%"));
    }

    @Test
    void fatorR_addsFgtsToPayroll() {
        // 160.000 × 1,08 = 172.800 / 600.000 = 28,8% → Anexo III: 600.000 × 13,5% − 17.640 = 63.360
        // (without FGTS it would be 26,7% → Anexo V: 600.000 × 19,5% − 9.900 = 107.100)
        RegimeResult atThreshold = regime(compare("600000", SERVICES_ANNEX_V, "160000", "0", "0", "0.05"),
                TaxRegime.SIMPLES_NACIONAL);
        assertThat(atThreshold.total()).isEqualByComparingTo("63360.00");
        assertThat(atThreshold.notes()).anyMatch(n -> n.contains("Anexo III"));

        // 155.000 × 1,08 = 167.400 / 600.000 = 27,9% → still Anexo V
        assertThat(regime(compare("600000", SERVICES_ANNEX_V, "155000", "0", "0", "0.05"),
                TaxRegime.SIMPLES_NACIONAL).total()).isEqualByComparingTo("107100.00");
    }

    @Test
    void annexV_appliesFatorR() {
        // 180.000 / 600.000 = 30% → Anexo III: 600.000 × 10,56% = 63.360
        RegimeResult simples = regime(compare("600000", SERVICES_ANNEX_V, "180000", "0", "0", "0.05"),
                TaxRegime.SIMPLES_NACIONAL);

        assertThat(simples.total()).isEqualByComparingTo("63360.00");
        assertThat(simples.notes()).anyMatch(n -> n.contains("Anexo III"));
    }

    @Test
    void simplesAboveSublimit_paysIcmsOutsideDas() {
        // DAS 4.000.000 × 9,55% = 382.000 (no ICMS share); ICMS 4.000.000 × 10% = 400.000
        RegimeResult simples = regime(compare("4000000", COMMERCE, "0", "0", "0", "0.10"), TaxRegime.SIMPLES_NACIONAL);

        assertThat(tax(simples, TaxType.ICMS)).isEqualByComparingTo("400000.00");
        assertThat(simples.total()).isEqualByComparingTo("782000.00");
    }

    @Test
    void revenueAbove78Million_onlyLucroRealIsEligible() {
        TaxComparisonResult r = compare("80000000", SERVICES, "0", "0", "0", "0");

        assertThat(regime(r, TaxRegime.LUCRO_PRESUMIDO).eligible()).isFalse();
        assertThat(regime(r, TaxRegime.LUCRO_PRESUMIDO).total()).isNull();
        assertThat(r.cheapest()).isEqualTo(TaxRegime.LUCRO_REAL);
    }

    @Test
    void defaultsPayrollChargesTo27_8Percent() {
        RegimeResult presumido = regime(comparator.compare(new TaxComparisonParameters(new BigDecimal("100000"),
                SERVICES, new BigDecimal("10000"), null, null, null, null)), TaxRegime.LUCRO_PRESUMIDO);

        assertThat(tax(presumido, TaxType.CPP)).isEqualByComparingTo("2780.00");
    }

    @Test
    void invalidInputs_throw() {
        assertThatThrownBy(() -> compare("100000", COMMERCE, "0", "1000", "2000", "0"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> compare("0", COMMERCE, "0", "0", "0", "0"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> compare("100000", COMMERCE, "-1", "0", "0", "0"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> compare("100000", COMMERCE, "0", "0", "0", "1.5"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
