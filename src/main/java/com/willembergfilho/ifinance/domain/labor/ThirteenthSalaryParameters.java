package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;

import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.orZero;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.requireNonNegative;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.requirePositive;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.requireRange;

/**
 * @param monthsWorked months of the year with 15+ days worked (Lei 4.090/1962, art. 1º §2º)
 */
public record ThirteenthSalaryParameters(
        BigDecimal monthlySalary,
        BigDecimal averageVariablePay,
        int monthsWorked,
        int dependents,
        Integer year
) {
    public ThirteenthSalaryParameters {
        requirePositive(monthlySalary, "Salário mensal");
        averageVariablePay = orZero(averageVariablePay);
        requireNonNegative(averageVariablePay, "Média de variáveis");
        requireRange(monthsWorked, 1, 12, "Meses trabalhados");
        requireRange(dependents, 0, 30, "Número de dependentes");
    }
}
