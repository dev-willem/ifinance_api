package com.willembergfilho.ifinance.domain.simulation;

import java.math.BigDecimal;
import java.util.List;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.round;

public record AmortizationSchedule(
        List<Installment> installments,
        BigDecimal totalPaid,
        BigDecimal totalInterest,
        CetResult cetResult
) {
    public static AmortizationSchedule of(List<Installment> installments, CetResult cetResult) {
        BigDecimal totalPaid = installments.stream()
                .map(Installment::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInterest = installments.stream()
                .map(Installment::interest)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AmortizationSchedule(installments, round(totalPaid), round(totalInterest), cetResult);
    }
}
