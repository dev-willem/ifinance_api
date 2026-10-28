package com.willembergfilho.ifinance.api.business;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Every rate is a fraction (0.18 = 18%) of the selling price.
 * @Digits bounds magnitude and scale, so extreme exponents (1e99999999) never reach BigDecimal arithmetic.
 */
public record MarkupRequest(
        @NotNull(message = "Informe o custo unitário.")
        @DecimalMin(value = "0.01", message = "O custo unitário deve ser de ao menos R$ 0,01.")
        @Digits(integer = 13, fraction = 4, message = "O custo unitário excede o limite de 13 dígitos e 4 casas decimais.") BigDecimal unitCost,
        @DecimalMin(value = "0", message = "O percentual de impostos não pode ser negativo.")
        @DecimalMax(value = "1", message = "O percentual de impostos não pode exceder 100%.")
        @Digits(integer = 1, fraction = 10, message = "O percentual de impostos aceita no máximo 10 casas decimais.") BigDecimal salesTaxRate,
        @DecimalMin(value = "0", message = "O percentual de comissão não pode ser negativo.")
        @DecimalMax(value = "1", message = "O percentual de comissão não pode exceder 100%.")
        @Digits(integer = 1, fraction = 10, message = "O percentual de comissão aceita no máximo 10 casas decimais.") BigDecimal commissionRate,
        @DecimalMin(value = "0", message = "A taxa de cartão não pode ser negativa.")
        @DecimalMax(value = "1", message = "A taxa de cartão não pode exceder 100%.")
        @Digits(integer = 1, fraction = 10, message = "A taxa de cartão aceita no máximo 10 casas decimais.") BigDecimal cardFeeRate,
        @DecimalMin(value = "0", message = "O percentual de despesas fixas não pode ser negativo.")
        @DecimalMax(value = "1", message = "O percentual de despesas fixas não pode exceder 100%.")
        @Digits(integer = 1, fraction = 10, message = "O percentual de despesas fixas aceita no máximo 10 casas decimais.") BigDecimal fixedExpenseRate,
        @DecimalMin(value = "0", message = "A margem de lucro desejada não pode ser negativa.")
        @DecimalMax(value = "1", message = "A margem de lucro desejada não pode exceder 100%.")
        @Digits(integer = 1, fraction = 10, message = "A margem de lucro desejada aceita no máximo 10 casas decimais.") BigDecimal desiredProfitRate,
        @Size(max = 20, message = "Informe no máximo 20 percentuais adicionais.") List<@Valid ExtraRate> extraRates,
        @Positive(message = "O preço atual deve ser maior que zero.")
        @Digits(integer = 13, fraction = 4, message = "O preço atual excede o limite de 13 dígitos e 4 casas decimais.") BigDecimal currentPrice
) {
    public record ExtraRate(
            @NotBlank(message = "Informe o nome de cada percentual adicional.")
            @Size(max = 60, message = "O nome do percentual adicional pode ter no máximo 60 caracteres.") String name,
            @NotNull(message = "Informe o valor de cada percentual adicional.")
            @DecimalMin(value = "0", message = "Percentuais adicionais não podem ser negativos.")
            @DecimalMax(value = "1", message = "Percentuais adicionais não podem exceder 100%.")
            @Digits(integer = 1, fraction = 10, message = "Percentuais adicionais aceitam no máximo 10 casas decimais.") BigDecimal rate
    ) {}
}
