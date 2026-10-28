package com.willembergfilho.ifinance.api.depreciation;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DepreciationCompareRequest(
        @NotNull(message = "Informe o valor do ativo.")
        @Positive(message = "O valor do ativo deve ser positivo.")
        @DecimalMax(value = "1000000000000", message = "O valor do ativo excede o limite permitido.")
        @Digits(integer = 13, fraction = 2, message = "O valor do ativo aceita no máximo 2 casas decimais.")
        BigDecimal assetValue,
        @NotNull(message = "Informe o valor residual.")
        @PositiveOrZero(message = "O valor residual não pode ser negativo.")
        @DecimalMax(value = "1000000000000", message = "O valor residual excede o limite permitido.")
        @Digits(integer = 13, fraction = 2, message = "O valor residual aceita no máximo 2 casas decimais.")
        BigDecimal residualValue,
        @Min(value = 1, message = "A vida útil deve ser de no mínimo 1 ano.")
        @Max(value = 50, message = "A vida útil deve ser de no máximo 50 anos.")
        int usefulLifeYears,
        @DecimalMin(value = "1.0", message = "O fator do saldo decrescente deve ser no mínimo 1.")
        @DecimalMax(value = "4.0", message = "O fator do saldo decrescente deve ser no máximo 4.")
        BigDecimal decliningFactor,
        @Min(value = 1, message = "O número de turnos deve estar entre 1 e 3.")
        @Max(value = 3, message = "O número de turnos deve estar entre 1 e 3.")
        Integer shiftsPerDay,
        @Min(value = 1, message = "Os meses do primeiro ano devem estar entre 1 e 12.")
        @Max(value = 12, message = "Os meses do primeiro ano devem estar entre 1 e 12.")
        Integer firstYearMonths
) {}
