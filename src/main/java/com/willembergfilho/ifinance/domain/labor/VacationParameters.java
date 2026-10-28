package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;

import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.orZero;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.requireNonNegative;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.requirePositive;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.requireRange;

/**
 * @param days     vacation days taken (5..30; CLT art. 134 §1)
 * @param soldDays abono pecuniário: at most 1/3 of the entitled days (days + soldDays; CLT art. 143)
 */
public record VacationParameters(
        BigDecimal monthlySalary,
        BigDecimal averageVariablePay,
        int days,
        int soldDays,
        int dependents,
        Integer year
) {
    public VacationParameters {
        requirePositive(monthlySalary, "Salário mensal");
        averageVariablePay = orZero(averageVariablePay);
        requireNonNegative(averageVariablePay, "Média de variáveis");
        requireRange(days, 5, 30, "Dias de férias");
        requireRange(soldDays, 0, 10, "Dias vendidos (abono)");
        requireRange(dependents, 0, 30, "Número de dependentes");
        if (days + soldDays > 30) {
            throw new IllegalArgumentException("Dias de férias somados aos dias vendidos não podem passar de 30.");
        }
        if (soldDays * 3 > days + soldDays) {
            throw new IllegalArgumentException("O abono pecuniário é limitado a 1/3 dos dias de férias a que o empregado tem direito.");
        }
    }
}
