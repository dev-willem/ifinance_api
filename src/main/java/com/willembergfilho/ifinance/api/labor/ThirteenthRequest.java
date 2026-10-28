package com.willembergfilho.ifinance.api.labor;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ThirteenthRequest(
        @NotNull(message = "Informe o salário mensal.")
        @Positive(message = "O salário mensal deve ser maior que zero.")
        @DecimalMax(value = "100000000", message = "Salário mensal acima do limite suportado.")
        BigDecimal monthlySalary,
        @PositiveOrZero(message = "A média de variáveis não pode ser negativa.") BigDecimal averageVariablePay,
        @NotNull(message = "Informe os meses trabalhados.")
        @Min(value = 1, message = "Informe ao menos 1 mês trabalhado.")
        @Max(value = 12, message = "Máximo de 12 meses trabalhados.")
        Integer monthsWorked,
        @Min(value = 0, message = "Dependentes não pode ser negativo.")
        @Max(value = 30, message = "Máximo de 30 dependentes.")
        Integer dependents,
        Integer year
) {}
