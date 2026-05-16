package com.willembergfilho.ifinance.api.simulation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.simulation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimulationResponse(
        UUID id,
        String name,
        AmortizationSystem amortizationSystem,
        BigDecimal principal,
        BigDecimal interestRate,
        RateType rateType,
        int term,
        Periodicity periodicity,
        boolean cetEnabled,
        boolean inflationCorrectionEnabled,
        String inflationIndex,
        SimulationStatus status,
        BigDecimal totalPaid,
        BigDecimal totalInterest,
        CetResultResponse cet,
        List<InstallmentResponse> installments,
        Instant createdAt
) {
    public static SimulationResponse from(Simulation simulation) {
        SimulationParameters p = simulation.getParameters();
        AmortizationSchedule schedule = simulation.getSchedule();

        List<InstallmentResponse> installments = schedule != null
                ? schedule.installments().stream().map(InstallmentResponse::from).toList()
                : List.of();

        return new SimulationResponse(
                simulation.getId(),
                p.name(),
                p.amortizationSystem(),
                p.principal(),
                p.interestRate(),
                p.rateType(),
                p.term(),
                p.periodicity(),
                p.cetEnabled(),
                p.inflationCorrectionEnabled(),
                p.inflationIndex() != null ? p.inflationIndex().name() : null,
                simulation.getStatus(),
                schedule != null ? schedule.totalPaid() : null,
                schedule != null ? schedule.totalInterest() : null,
                schedule != null ? CetResultResponse.from(schedule.cetResult()) : null,
                installments,
                simulation.getCreatedAt()
        );
    }
}
