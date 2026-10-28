package com.willembergfilho.ifinance.domain.labor;

import com.willembergfilho.ifinance.domain.math.CalculationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NetSalaryCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void gross5000_netAndEmployerCost() {
        // INSS 501.51; IRRF 312.89 - 312.89 reduction = 0; net 4498.49; FGTS 400
        NetSalaryResult r = NetSalaryCalculator.calculate(new NetSalaryParameters(bd("5000"), 0, null, null, null, 2026));

        assertThat(r.inss().total()).isEqualByComparingTo("501.51");
        assertThat(r.irrf().tax()).isEqualByComparingTo("0");
        assertThat(r.netSalary()).isEqualByComparingTo("4498.49");
        assertThat(r.fgts()).isEqualByComparingTo("400.00");
        // employer: INSS 20% 1000; 13º 5000/12 = 416.67; férias+1/3 5000/9 = 555.56;
        // charges (416.67 + 555.56) x 28% = 272.22; total 7644.45
        NetSalaryResult.EmployerCost c = r.employerCost();
        assertThat(c.employerInss()).isEqualByComparingTo("1000.00");
        assertThat(c.thirteenthProvision()).isEqualByComparingTo("416.67");
        assertThat(c.vacationProvision()).isEqualByComparingTo("555.56");
        assertThat(c.provisionCharges()).isEqualByComparingTo("272.22");
        assertThat(c.total()).isEqualByComparingTo("7644.45");
        assertThat(c.costToSalaryRatio()).isEqualByComparingTo("1.5289");
    }

    @Test
    void gross6000_dependentAlimonyAndOtherDiscounts() {
        // INSS 641.51; legal = 641.51 + 189.59 + 500 = 1331.10; base 4668.90 x 27.5% - 908.73 = 375.22;
        // reduction 179.75 -> IRRF 195.47; net = 6000 - 641.51 - 195.47 - 500 - 200 = 4463.02
        NetSalaryResult r = NetSalaryCalculator.calculate(
                new NetSalaryParameters(bd("6000"), 1, bd("500"), bd("200"), bd("0.20"), 2026));

        assertThat(r.irrf().deductionMethod()).isEqualTo(IrrfResult.DeductionMethod.LEGAL);
        assertThat(r.irrf().taxBeforeReduction()).isEqualByComparingTo("375.22");
        assertThat(r.irrf().tax()).isEqualByComparingTo("195.47");
        assertThat(r.totalDeductions()).isEqualByComparingTo("1536.98");
        assertThat(r.netSalary()).isEqualByComparingTo("4463.02");
        assertThat(r.totalTaxEffectiveRate()).isEqualByComparingTo("0.1395"); // 836.98 / 6000
        assertThat(r.lines()).extracting(PayslipLine::description)
                .containsExactly("Salário base", "INSS", "IRRF", "Pensão alimentícia", "Outros descontos");
    }

    @Test
    void simplesNacional_zeroEmployerRate() {
        NetSalaryResult r = NetSalaryCalculator.calculate(new NetSalaryParameters(bd("3000"), 0, null, null, BigDecimal.ZERO, null));

        // 3000 + 240 FGTS + 250 (13º) + 333.33 (férias) + 583.33 x 8% (46.67) = 3870.00
        assertThat(r.year()).isEqualTo(2026);
        assertThat(r.employerCost().employerInss()).isEqualByComparingTo("0");
        assertThat(r.employerCost().total()).isEqualByComparingTo("3870.00");
    }

    @Test
    void discountsAboveGross_rejected() {
        assertThatThrownBy(() -> NetSalaryCalculator.calculate(
                new NetSalaryParameters(bd("2000"), 0, null, bd("3000"), null, null)))
                .isInstanceOf(CalculationException.class);
    }

    @Test
    void invalidInputs_rejected() {
        assertThatThrownBy(() -> new NetSalaryParameters(BigDecimal.ZERO, 0, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NetSalaryParameters(bd("3000"), -1, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NetSalaryParameters(bd("3000"), 0, bd("-1"), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NetSalaryParameters(bd("3000"), 0, null, null, BigDecimal.ONE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
