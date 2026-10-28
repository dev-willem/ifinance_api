package com.willembergfilho.ifinance.api.tax;

import com.willembergfilho.ifinance.domain.tax.SimplesAnnex;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

// @Digits bounds the BigDecimal scale: a short literal like 1e10000000 would otherwise expand to millions of digits
public record SimplesRequest(
        @NotNull(message = "Informe a RBT12.")
        @Positive(message = "A RBT12 deve ser maior que zero.")
        @Digits(integer = 15, fraction = 2, message = "RBT12 fora do limite (até 15 dígitos e 2 casas decimais).") BigDecimal rbt12,
        @NotNull(message = "Informe a receita do mês.")
        @PositiveOrZero(message = "A receita do mês não pode ser negativa.")
        @Digits(integer = 15, fraction = 2, message = "Receita do mês fora do limite (até 15 dígitos e 2 casas decimais).") BigDecimal monthlyRevenue,
        @NotNull(message = "Informe o anexo.") SimplesAnnex annex,
        @PositiveOrZero(message = "A folha de salários não pode ser negativa.")
        @Digits(integer = 15, fraction = 2, message = "Folha de salários fora do limite (até 15 dígitos e 2 casas decimais).") BigDecimal payroll12
) {}
