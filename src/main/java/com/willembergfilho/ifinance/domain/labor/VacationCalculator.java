package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.THIRTY;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.THREE;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.fraction;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.percentLabel;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.round;
import static java.math.BigDecimal.ZERO;

/**
 * Vacation receipt. Vacation pay + 1/3 constitucional (CF art. 7º, XVII) is taxed on its own: IRRF on
 * férias is withheld apart from the month's other income, and INSS is computed as if the vacation filled
 * the whole competence (salary for other days of a split month is not added). Abono pecuniário (CLT art. 143)
 * and its 1/3 are exempt from INSS (Lei 8.212/1991 art. 28 §9º, "e", 6) and IRRF (IN RFB 936/2009).
 */
public final class VacationCalculator {

    private VacationCalculator() {}

    public static VacationResult calculate(VacationParameters p) {
        LaborTaxTable table = LaborTaxTables.forYear(p.year());
        BigDecimal remuneration = p.monthlySalary().add(p.averageVariablePay());

        BigDecimal vacationPay = fraction(remuneration, p.days(), THIRTY);
        BigDecimal vacationBonus = round(vacationPay.divide(THREE, MC));
        BigDecimal allowance = fraction(remuneration, p.soldDays(), THIRTY);
        BigDecimal allowanceBonus = round(allowance.divide(THREE, MC));
        BigDecimal taxable = vacationPay.add(vacationBonus);

        InssResult inss = PayrollTaxes.inss(taxable, table);
        IrrfResult irrf = PayrollTaxes.irrf(taxable, inss.total(), p.dependents(), ZERO, table);

        BigDecimal gross = taxable.add(allowance).add(allowanceBonus);
        BigDecimal deductions = inss.total().add(irrf.tax());

        List<PayslipLine> lines = new ArrayList<>();
        lines.add(PayslipLine.earning("Férias", p.days() + " dias", vacationPay, true));
        lines.add(PayslipLine.earning("1/3 constitucional de férias", null, vacationBonus, true));
        if (allowance.signum() > 0) {
            lines.add(PayslipLine.earning("Abono pecuniário", p.soldDays() + " dias", allowance, false));
            lines.add(PayslipLine.earning("1/3 sobre abono pecuniário", null, allowanceBonus, false));
        }
        lines.add(PayslipLine.deduction("INSS sobre férias", null, inss.total()));
        if (irrf.tax().signum() > 0) {
            lines.add(PayslipLine.deduction("IRRF sobre férias", percentLabel(irrf.rate()), irrf.tax()));
        }

        return new VacationResult(table.year(), round(remuneration), p.days(), p.soldDays(), vacationPay,
                vacationBonus, allowance, allowanceBonus, taxable, inss, irrf, gross, deductions,
                gross.subtract(deductions), lines);
    }
}
