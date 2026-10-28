package com.willembergfilho.ifinance.api.labor;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record NetSalaryRequest(
        @NotNull(message = "Informe o salário bruto.")
        @Positive(message = "O salário bruto deve ser maior que zero.")
        @DecimalMax(value = "100000000", message = "Salário bruto acima do limite suportado.")
        BigDecimal grossSalary,
        @Min(value = 0, message = "Dependentes não pode ser negativo.")
        @Max(value = 30, message = "Máximo de 30 dependentes.")
        Integer dependents,
        @PositiveOrZero(message = "A pensão alimentícia não pode ser negativa.") BigDecimal alimony,
        @PositiveOrZero(message = "Outros descontos não podem ser negativos.") BigDecimal otherDiscounts,
        @DecimalMin(value = "0", message = "A alíquota patronal não pode ser negativa.")
        @DecimalMax(value = "0.5", message = "A alíquota patronal deve ser no máximo 50%.")
        BigDecimal employerInssRate,
        Integer year
) {}
