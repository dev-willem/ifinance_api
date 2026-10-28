package com.willembergfilho.ifinance.api.labor;

import com.willembergfilho.ifinance.domain.labor.NoticeType;
import com.willembergfilho.ifinance.domain.labor.TerminationType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TerminationRequest(
        @NotNull(message = "Informe o salário mensal.")
        @Positive(message = "O salário mensal deve ser maior que zero.")
        @DecimalMax(value = "100000000", message = "Salário mensal acima do limite suportado.")
        BigDecimal monthlySalary,
        @NotNull(message = "Informe a data de admissão.") LocalDate admissionDate,
        @NotNull(message = "Informe a data de desligamento.") LocalDate terminationDate,
        @NotNull(message = "Informe o tipo de desligamento.") TerminationType terminationType,
        NoticeType noticeType,
        @Min(value = 0, message = "Férias vencidas não pode ser negativo.")
        @Max(value = 2, message = "Máximo de 2 períodos de férias vencidas.")
        Integer expiredVacationPeriods,
        @PositiveOrZero(message = "O saldo do FGTS não pode ser negativo.") BigDecimal fgtsBalance,
        @Min(value = 0, message = "Dependentes não pode ser negativo.")
        @Max(value = 30, message = "Máximo de 30 dependentes.")
        Integer dependents,
        Integer year
) {}
