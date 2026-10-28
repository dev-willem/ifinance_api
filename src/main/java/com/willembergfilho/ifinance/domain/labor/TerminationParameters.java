package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.requireNonNegative;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.requirePositive;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.requireRange;

/**
 * @param terminationDate        last day of the contract (for worked notice, the last day of the notice)
 * @param noticeType             ignored for WITH_CAUSE and END_OF_FIXED_TERM
 * @param expiredVacationPeriods complete acquisition periods whose vacation was not taken (férias vencidas)
 * @param fgtsBalance            FGTS statement balance; null = estimate from the salary
 * @param year                   tax table year; null = year of the termination date
 */
public record TerminationParameters(
        BigDecimal monthlySalary,
        LocalDate admissionDate,
        LocalDate terminationDate,
        TerminationType terminationType,
        NoticeType noticeType,
        int expiredVacationPeriods,
        BigDecimal fgtsBalance,
        int dependents,
        Integer year
) {
    public TerminationParameters {
        requirePositive(monthlySalary, "Salário mensal");
        if (admissionDate == null || terminationDate == null) {
            throw new IllegalArgumentException("Informe as datas de admissão e de desligamento.");
        }
        if (admissionDate.isAfter(terminationDate)) {
            throw new IllegalArgumentException("A data de admissão deve ser anterior à data de desligamento.");
        }
        if (terminationType == null) {
            throw new IllegalArgumentException("Informe o tipo de desligamento.");
        }
        requireRange(expiredVacationPeriods, 0, 2, "Períodos de férias vencidas");
        requireRange(dependents, 0, 30, "Número de dependentes");
        if (fgtsBalance != null) requireNonNegative(fgtsBalance, "Saldo do FGTS");

        int completeYears = Period.between(admissionDate, terminationDate).getYears();
        if (expiredVacationPeriods > completeYears) {
            throw new IllegalArgumentException("Férias vencidas não podem exceder os períodos aquisitivos completos ("
                    + completeYears + ").");
        }
        switch (terminationType) {
            case WITHOUT_CAUSE, MUTUAL_AGREEMENT -> {
                if (noticeType != NoticeType.WORKED && noticeType != NoticeType.INDEMNIFIED) {
                    throw new IllegalArgumentException("Na dispensa sem justa causa ou por acordo, o aviso prévio é trabalhado ou indenizado.");
                }
            }
            case RESIGNATION -> {
                if (noticeType != NoticeType.WORKED && noticeType != NoticeType.NOT_SERVED) {
                    throw new IllegalArgumentException("No pedido de demissão, o aviso prévio é trabalhado ou não cumprido.");
                }
            }
            case END_OF_FIXED_TERM -> {
                if (admissionDate.plusYears(2).isBefore(terminationDate)) {
                    throw new IllegalArgumentException("Contrato por prazo determinado não pode exceder 2 anos (CLT, art. 445).");
                }
            }
            case WITH_CAUSE -> { }
        }
    }
}
