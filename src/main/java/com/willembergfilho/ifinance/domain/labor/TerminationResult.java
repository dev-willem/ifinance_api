package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @param noticeDays            legal notice period (Lei 12.506/2011); 0 when not applicable
 * @param indemnifiedNoticeDays notice days paid and projected into the contract (CLT art. 487 §1º)
 * @param projectedEndDate      termination date + indemnified notice, used for 13º and vacation avos
 * @param thirteenthMonths      13º avos paid, including those the projection carries into the next year
 * @param grossTotal            statement earnings; the FGTS fine is deposited in the FGTS account, not included
 */
public record TerminationResult(
        int year,
        TerminationType terminationType,
        NoticeType noticeType,
        int completeYearsOfService,
        int noticeDays,
        int indemnifiedNoticeDays,
        LocalDate projectedEndDate,
        int salaryBalanceDays,
        int thirteenthMonths,
        int vacationMonths,
        List<PayslipLine> lines,
        InssResult salaryInss,
        IrrfResult salaryIrrf,
        InssResult thirteenthInss,
        IrrfResult thirteenthIrrf,
        FgtsSummary fgts,
        boolean unemploymentInsuranceEligible,
        List<String> notes,
        BigDecimal grossTotal,
        BigDecimal totalDeductions,
        BigDecimal netTotal
) {
    /**
     * @param terminationDeposit FGTS (8%) on the termination-month items: salary balance, indemnified notice, 13º
     * @param withdrawableAmount share of balance + termination deposit + fine the employee may withdraw
     */
    public record FgtsSummary(
            BigDecimal balance,
            boolean balanceEstimated,
            BigDecimal terminationDeposit,
            BigDecimal fineRate,
            BigDecimal fine,
            BigDecimal withdrawalRate,
            BigDecimal withdrawableAmount
    ) {}
}
