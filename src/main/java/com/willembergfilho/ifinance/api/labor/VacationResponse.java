package com.willembergfilho.ifinance.api.labor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.labor.InssResult;
import com.willembergfilho.ifinance.domain.labor.IrrfResult;
import com.willembergfilho.ifinance.domain.labor.PayslipLine;
import com.willembergfilho.ifinance.domain.labor.VacationResult;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VacationResponse(
        int year,
        BigDecimal remuneration,
        int days,
        int soldDays,
        BigDecimal vacationPay,
        BigDecimal vacationBonus,
        BigDecimal allowance,
        BigDecimal allowanceBonus,
        BigDecimal taxableTotal,
        InssResult inss,
        IrrfResult irrf,
        BigDecimal grossTotal,
        BigDecimal totalDeductions,
        BigDecimal netTotal,
        List<PayslipLine> lines
) {
    public static VacationResponse from(VacationResult r) {
        return new VacationResponse(
                r.year(),
                r.remuneration(),
                r.days(),
                r.soldDays(),
                r.vacationPay(),
                r.vacationBonus(),
                r.allowance(),
                r.allowanceBonus(),
                r.taxableTotal(),
                r.inss(),
                r.irrf(),
                r.grossTotal(),
                r.totalDeductions(),
                r.netTotal(),
                r.lines()
        );
    }
}
