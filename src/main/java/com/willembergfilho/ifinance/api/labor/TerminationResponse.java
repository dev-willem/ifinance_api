package com.willembergfilho.ifinance.api.labor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.labor.InssResult;
import com.willembergfilho.ifinance.domain.labor.IrrfResult;
import com.willembergfilho.ifinance.domain.labor.NoticeType;
import com.willembergfilho.ifinance.domain.labor.PayslipLine;
import com.willembergfilho.ifinance.domain.labor.TerminationResult;
import com.willembergfilho.ifinance.domain.labor.TerminationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TerminationResponse(
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
        TerminationResult.FgtsSummary fgts,
        boolean unemploymentInsuranceEligible,
        List<String> notes,
        BigDecimal grossTotal,
        BigDecimal totalDeductions,
        BigDecimal netTotal
) {
    public static TerminationResponse from(TerminationResult r) {
        return new TerminationResponse(
                r.year(),
                r.terminationType(),
                r.noticeType(),
                r.completeYearsOfService(),
                r.noticeDays(),
                r.indemnifiedNoticeDays(),
                r.projectedEndDate(),
                r.salaryBalanceDays(),
                r.thirteenthMonths(),
                r.vacationMonths(),
                r.lines(),
                r.salaryInss(),
                r.salaryIrrf(),
                r.thirteenthInss(),
                r.thirteenthIrrf(),
                r.fgts(),
                r.unemploymentInsuranceEligible(),
                r.notes(),
                r.grossTotal(),
                r.totalDeductions(),
                r.netTotal()
        );
    }
}
