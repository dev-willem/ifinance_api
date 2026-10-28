package com.willembergfilho.ifinance.api.business;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** @Digits bounds magnitude and scale, so extreme exponents (1e99999999) never reach BigDecimal arithmetic. */
public record IncomeStatementRequest(
        @NotNull(message = "Informe a receita bruta.")
        @PositiveOrZero(message = "A receita bruta não pode ser negativa.")
        @Digits(integer = 13, fraction = 4, message = "A receita bruta excede o limite de 13 dígitos e 4 casas decimais.") BigDecimal grossRevenue,
        @DecimalMin(value = "0", message = "O percentual de deduções não pode ser negativo.")
        @DecimalMax(value = "1", message = "O percentual de deduções não pode exceder 100%.")
        @Digits(integer = 1, fraction = 10, message = "O percentual de deduções aceita no máximo 10 casas decimais.") BigDecimal salesDeductionsRate,
        @NotNull(message = "Informe o CMV/CPV/CSP.")
        @PositiveOrZero(message = "O CMV/CPV/CSP não pode ser negativo.")
        @Digits(integer = 13, fraction = 4, message = "O CMV/CPV/CSP excede o limite de 13 dígitos e 4 casas decimais.") BigDecimal costOfGoodsSold,
        @PositiveOrZero(message = "As despesas com vendas não podem ser negativas.")
        @Digits(integer = 13, fraction = 4, message = "As despesas com vendas excedem o limite de 13 dígitos e 4 casas decimais.") BigDecimal sellingExpenses,
        @PositiveOrZero(message = "As despesas administrativas não podem ser negativas.")
        @Digits(integer = 13, fraction = 4, message = "As despesas administrativas excedem o limite de 13 dígitos e 4 casas decimais.") BigDecimal administrativeExpenses,
        @PositiveOrZero(message = "As outras despesas operacionais não podem ser negativas.")
        @Digits(integer = 13, fraction = 4, message = "As outras despesas operacionais excedem o limite de 13 dígitos e 4 casas decimais.") BigDecimal otherOperatingExpenses,
        @PositiveOrZero(message = "A depreciação e amortização não pode ser negativa.")
        @Digits(integer = 13, fraction = 4, message = "A depreciação e amortização excede o limite de 13 dígitos e 4 casas decimais.") BigDecimal depreciationAmortization,
        @PositiveOrZero(message = "A receita financeira não pode ser negativa.")
        @Digits(integer = 13, fraction = 4, message = "A receita financeira excede o limite de 13 dígitos e 4 casas decimais.") BigDecimal financialRevenue,
        @PositiveOrZero(message = "A despesa financeira não pode ser negativa.")
        @Digits(integer = 13, fraction = 4, message = "A despesa financeira excede o limite de 13 dígitos e 4 casas decimais.") BigDecimal financialExpenses,
        @DecimalMin(value = "0", message = "A alíquota de IR/CSLL não pode ser negativa.")
        @DecimalMax(value = "1", message = "A alíquota de IR/CSLL não pode exceder 100%.")
        @Digits(integer = 1, fraction = 10, message = "A alíquota de IR/CSLL aceita no máximo 10 casas decimais.") BigDecimal incomeTaxRate,
        @Min(value = 1, message = "A projeção deve ter ao menos 1 mês.")
        @Max(value = 60, message = "A projeção pode ter no máximo 60 meses.") Integer months,
        @DecimalMin(value = "-1", inclusive = false, message = "O crescimento mensal deve ser maior que -100%.")
        @DecimalMax(value = "1", message = "O crescimento mensal pode ser de no máximo 100%.")
        @Digits(integer = 1, fraction = 10, message = "O crescimento mensal aceita no máximo 10 casas decimais.") BigDecimal monthlyRevenueGrowth
) {}
