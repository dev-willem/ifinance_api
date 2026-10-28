package com.willembergfilho.ifinance.api.labor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.labor.InssResult;
import com.willembergfilho.ifinance.domain.labor.IrrfResult;
import com.willembergfilho.ifinance.domain.labor.PayslipLine;
import com.willembergfilho.ifinance.domain.labor.ThirteenthSalaryResult;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ThirteenthResponse(
        int year,
        BigDecimal remuneration,
        int monthsWorked,
        BigDecimal grossTotal,
        BigDecimal firstInstallment,
        BigDecimal secondInstallmentGross,
        InssResult inss,
        IrrfResult irrf,
        BigDecimal secondInstallment,
        BigDecimal totalDeductions,
        BigDecimal netTotal,
        List<PayslipLine> lines
) {
    public static ThirteenthResponse from(ThirteenthSalaryResult r) {
        return new ThirteenthResponse(
                r.year(),
                r.remuneration(),
                r.monthsWorked(),
                r.grossTotal(),
                r.firstInstallment(),
                r.secondInstallmentGross(),
                r.inss(),
                r.irrf(),
                r.secondInstallment(),
                r.totalDeductions(),
                r.netTotal(),
                r.lines()
        );
    }
}
