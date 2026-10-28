package com.willembergfilho.ifinance.api.business;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** @Digits bounds magnitude and scale, so extreme exponents (1e99999999) never reach BigDecimal arithmetic. */
public record BreakEvenRequest(
        @NotNull(message = "Informe os custos fixos.")
        @PositiveOrZero(message = "Os custos fixos não podem ser negativos.")
        @Digits(integer = 13, fraction = 4, message = "Os custos fixos excedem o limite de 13 dígitos e 4 casas decimais.") BigDecimal fixedCosts,
        @NotNull(message = "Informe o preço de venda unitário.")
        @Positive(message = "O preço de venda unitário deve ser maior que zero.")
        @Digits(integer = 13, fraction = 4, message = "O preço de venda unitário excede o limite de 13 dígitos e 4 casas decimais.") BigDecimal unitPrice,
        @NotNull(message = "Informe o custo variável unitário.")
        @PositiveOrZero(message = "O custo variável unitário não pode ser negativo.")
        @Digits(integer = 13, fraction = 4, message = "O custo variável unitário excede o limite de 13 dígitos e 4 casas decimais.") BigDecimal unitVariableCost,
        @PositiveOrZero(message = "A depreciação não pode ser negativa.")
        @Digits(integer = 13, fraction = 4, message = "A depreciação excede o limite de 13 dígitos e 4 casas decimais.") BigDecimal depreciation,
        @PositiveOrZero(message = "O lucro desejado não pode ser negativo.")
        @Digits(integer = 13, fraction = 4, message = "O lucro desejado excede o limite de 13 dígitos e 4 casas decimais.") BigDecimal desiredProfit,
        @Positive(message = "A quantidade atual deve ser maior que zero.")
        @Digits(integer = 12, fraction = 4, message = "A quantidade atual excede o limite de 12 dígitos e 4 casas decimais.") BigDecimal currentQuantity
) {}
