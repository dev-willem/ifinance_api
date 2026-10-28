package com.willembergfilho.ifinance.api.risk;

import com.willembergfilho.ifinance.domain.risk.Periodicity;
import com.willembergfilho.ifinance.domain.risk.RiskInputType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record RiskMetricsRequest(
        @NotNull(message = "Informe a série de valores.")
        @Size(min = 2, max = 5000, message = "A série deve ter entre 2 e 5000 valores.")
        List<@NotNull(message = "A série não pode conter valores vazios.") Double> values,

        @NotNull(message = "Informe o tipo de entrada (RETURNS ou PRICES).")
        RiskInputType inputType,

        @Size(min = 2, max = 5000, message = "O benchmark deve ter entre 2 e 5000 valores.")
        List<@NotNull(message = "O benchmark não pode conter valores vazios.") Double> benchmarkValues,

        @NotNull(message = "Informe a periodicidade (DAILY, WEEKLY, MONTHLY ou ANNUAL).")
        Periodicity periodicity,

        @DecimalMin(value = "-0.99", message = "A taxa livre de risco deve ser maior que -99% a.a.")
        @DecimalMax(value = "10", message = "A taxa livre de risco deve ser no máximo 1000% a.a.")
        BigDecimal riskFreeAnnualRate,

        @DecimalMin(value = "0.5", message = "O nível de confiança deve estar entre 50% e 99,9%.")
        @DecimalMax(value = "0.999", message = "O nível de confiança deve estar entre 50% e 99,9%.")
        BigDecimal confidenceLevel
) {}
