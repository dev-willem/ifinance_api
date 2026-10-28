package com.willembergfilho.ifinance.domain.labor;

import com.willembergfilho.ifinance.domain.math.CalculationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.FGTS_RATE;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.TWELVE;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.percentLabel;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.ratio;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.round;

/** Monthly CLT payslip: gross to net, plus the employer's monthly cost. */
public final class NetSalaryCalculator {

    private NetSalaryCalculator() {}

    public static NetSalaryResult calculate(NetSalaryParameters p) {
        LaborTaxTable table = LaborTaxTables.forYear(p.year());
        BigDecimal gross = p.grossSalary();

        InssResult inss = PayrollTaxes.inss(gross, table);
        IrrfResult irrf = PayrollTaxes.irrf(gross, inss.total(), p.dependents(), p.alimony(), table);

        BigDecimal deductions = inss.total().add(irrf.tax()).add(p.alimony()).add(p.otherDiscounts());
        BigDecimal net = round(gross.subtract(deductions));
        if (net.signum() < 0) {
            throw new CalculationException("Os descontos informados superam o salário bruto.");
        }
        BigDecimal fgts = round(gross.multiply(FGTS_RATE));

        List<PayslipLine> lines = new ArrayList<>();
        lines.add(PayslipLine.earning("Salário base", "30 dias", round(gross), true));
        lines.add(PayslipLine.deduction("INSS", percentLabel(inss.effectiveRate()) + " efetiva", inss.total()));
        if (irrf.tax().signum() > 0) {
            lines.add(PayslipLine.deduction("IRRF", percentLabel(irrf.rate()), irrf.tax()));
        }
        if (p.alimony().signum() > 0) {
            lines.add(PayslipLine.deduction("Pensão alimentícia", null, round(p.alimony())));
        }
        if (p.otherDiscounts().signum() > 0) {
            lines.add(PayslipLine.deduction("Outros descontos", null, round(p.otherDiscounts())));
        }

        return new NetSalaryResult(table.year(), round(gross), inss, irrf, round(p.alimony()),
                round(p.otherDiscounts()), round(deductions), net, fgts,
                ratio(inss.total().add(irrf.tax()), gross),
                employerCost(gross, fgts, p.employerInssRate()), lines);
    }

    private static NetSalaryResult.EmployerCost employerCost(BigDecimal gross, BigDecimal fgts, BigDecimal employerRate) {
        BigDecimal employerInss = round(gross.multiply(employerRate));
        BigDecimal thirteenth = round(gross.divide(TWELVE, MC));
        BigDecimal vacation = round(gross.divide(BigDecimal.valueOf(9), MC)); // (1 + 1/3) / 12 = 1/9
        BigDecimal charges = round(thirteenth.add(vacation).multiply(FGTS_RATE.add(employerRate)));
        BigDecimal total = round(gross).add(fgts).add(employerInss).add(thirteenth).add(vacation).add(charges);
        return new NetSalaryResult.EmployerCost(round(gross), fgts, employerRate, employerInss,
                thirteenth, vacation, charges, total, ratio(total, gross));
    }
}
