package com.willembergfilho.ifinance.api.credit;

import com.willembergfilho.ifinance.domain.simulation.AmortizationSystem;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreditCapacityRequest(
        @NotNull(message = "A renda mensal é obrigatória.")
        @Positive(message = "A renda mensal deve ser maior que zero.")
        @DecimalMax(value = "1000000000", message = "A renda mensal excede o limite permitido.")
        BigDecimal monthlyIncome,

        @NotNull(message = "As dívidas mensais atuais são obrigatórias.")
        @PositiveOrZero(message = "As dívidas mensais atuais não podem ser negativas.")
        @DecimalMax(value = "1000000000", message = "As dívidas mensais excedem o limite permitido.")
        BigDecimal existingMonthlyDebts,

        @DecimalMin(value = "0.05", message = "O comprometimento máximo deve ser de pelo menos 5% da renda.")
        @DecimalMax(value = "0.70", message = "O comprometimento máximo deve ser de no máximo 70% da renda.")
        BigDecimal maxCommitmentRate,

        @NotNull(message = "A taxa de juros anual é obrigatória.")
        @DecimalMin(value = "0", message = "A taxa de juros anual não pode ser negativa.")
        @DecimalMax(value = "20", message = "A taxa de juros anual deve ser de no máximo 2000%.")
        BigDecimal annualInterestRate,

        @NotNull(message = "O prazo é obrigatório.")
        @Min(value = 1, message = "O prazo deve ser de pelo menos 1 mês.")
        @Max(value = 480, message = "O prazo deve ser de no máximo 480 meses.")
        Integer termMonths,

        @NotNull(message = "O sistema de amortização é obrigatório.")
        AmortizationSystem amortizationSystem,

        @DecimalMin(value = "0.01", message = "O valor desejado deve ser de pelo menos R$ 0,01.")
        @DecimalMax(value = "100000000000", message = "O valor desejado excede o limite permitido.")
        BigDecimal desiredLoanAmount
) {}
