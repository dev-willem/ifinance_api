package com.willembergfilho.ifinance.api.labor;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record VacationRequest(
        @NotNull(message = "Informe o salário mensal.")
        @Positive(message = "O salário mensal deve ser maior que zero.")
        @DecimalMax(value = "100000000", message = "Salário mensal acima do limite suportado.")
        BigDecimal monthlySalary,
        @PositiveOrZero(message = "A média de variáveis não pode ser negativa.") BigDecimal averageVariablePay,
        @NotNull(message = "Informe os dias de férias.")
        @Min(value = 5, message = "As férias devem ter ao menos 5 dias.")
        @Max(value = 30, message = "As férias têm no máximo 30 dias.")
        Integer days,
        @Min(value = 0, message = "Dias vendidos não pode ser negativo.")
        @Max(value = 10, message = "É possível vender no máximo 10 dias.")
        Integer soldDays,
        @Min(value = 0, message = "Dependentes não pode ser negativo.")
        @Max(value = 30, message = "Máximo de 30 dependentes.")
        Integer dependents,
        Integer year
) {}
