package com.willembergfilho.ifinance.api.tax;

import com.willembergfilho.ifinance.domain.tax.BusinessActivity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Rates are fractions (0.278 = 27,8%). Optional fields default to zero, except payrollChargesRate (27,8%).
 * {@code @Digits} bounds the BigDecimal scale so exponent literals (1e10000000, 1e-10000000) cannot blow up rounding;
 * rates allow 20 decimals because the UI sends percent / 100 with binary float noise (1.1 / 100 = 0.011000000000000001).
 */
public record TaxCompareRequest(
        @NotNull(message = "Informe a receita anual.")
        @Positive(message = "A receita anual deve ser maior que zero.")
        @Digits(integer = 15, fraction = 2, message = "Receita anual fora do limite (até 15 dígitos e 2 casas decimais).") BigDecimal annualRevenue,
        @NotNull(message = "Informe a atividade.") BusinessActivity activity,
        @PositiveOrZero(message = "A folha anual não pode ser negativa.")
        @Digits(integer = 15, fraction = 2, message = "Folha anual fora do limite (até 15 dígitos e 2 casas decimais).") BigDecimal annualPayroll,
        @DecimalMin(value = "0", message = "Os encargos sobre a folha não podem ser negativos.")
        @DecimalMax(value = "1", message = "Os encargos sobre a folha devem ser no máximo 100%.")
        @Digits(integer = 1, fraction = 20, message = "Encargos sobre a folha com casas decimais demais.") BigDecimal payrollChargesRate,
        @PositiveOrZero(message = "Os custos dedutíveis não podem ser negativos.")
        @Digits(integer = 15, fraction = 2, message = "Custos dedutíveis fora do limite (até 15 dígitos e 2 casas decimais).") BigDecimal deductibleExpenses,
        @PositiveOrZero(message = "As compras com crédito não podem ser negativas.")
        @Digits(integer = 15, fraction = 2, message = "Compras com crédito fora do limite (até 15 dígitos e 2 casas decimais).") BigDecimal creditablePurchases,
        @DecimalMin(value = "0", message = "A alíquota de ICMS/ISS não pode ser negativa.")
        @DecimalMax(value = "1", message = "A alíquota de ICMS/ISS deve ser no máximo 100%.")
        @Digits(integer = 1, fraction = 20, message = "Alíquota de ICMS/ISS com casas decimais demais.") BigDecimal salesTaxRate
) {}
