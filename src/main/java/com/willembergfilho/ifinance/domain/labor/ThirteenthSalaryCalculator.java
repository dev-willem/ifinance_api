package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.TWELVE;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.fraction;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.percentLabel;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.round;
import static java.math.BigDecimal.ZERO;

/**
 * 13º salário (Lei 4.090/1962, Lei 4.749/1965). INSS is computed apart from the monthly salary; IRRF is
 * exclusive at source on the full 13º, and the Lei 15.270/2025 reduction also applies to it (art. 3º-A §3º).
 */
public final class ThirteenthSalaryCalculator {

    private ThirteenthSalaryCalculator() {}

    public static ThirteenthSalaryResult calculate(ThirteenthSalaryParameters p) {
        LaborTaxTable table = LaborTaxTables.forYear(p.year());
        BigDecimal remuneration = p.monthlySalary().add(p.averageVariablePay());

        BigDecimal gross = fraction(remuneration, p.monthsWorked(), TWELVE);
        BigDecimal first = round(gross.divide(BigDecimal.TWO, MC));
        BigDecimal secondGross = gross.subtract(first);

        InssResult inss = PayrollTaxes.inss(gross, table);
        IrrfResult irrf = PayrollTaxes.irrf(gross, inss.total(), p.dependents(), ZERO, table);
        BigDecimal deductions = inss.total().add(irrf.tax());

        List<PayslipLine> lines = new ArrayList<>();
        lines.add(PayslipLine.earning("13º salário", p.monthsWorked() + "/12 avos", gross, true));
        lines.add(PayslipLine.deduction("Adiantamento (1ª parcela)", "50%", first));
        lines.add(PayslipLine.deduction("INSS sobre 13º", null, inss.total()));
        if (irrf.tax().signum() > 0) {
            lines.add(PayslipLine.deduction("IRRF sobre 13º", percentLabel(irrf.rate()), irrf.tax()));
        }

        return new ThirteenthSalaryResult(table.year(), round(remuneration), p.monthsWorked(), gross, first,
                secondGross, inss, irrf, secondGross.subtract(deductions), deductions,
                gross.subtract(deductions), lines);
    }
}
