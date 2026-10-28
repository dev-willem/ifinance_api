package com.willembergfilho.ifinance.api.labor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.labor.InssResult;
import com.willembergfilho.ifinance.domain.labor.IrrfResult;
import com.willembergfilho.ifinance.domain.labor.NetSalaryResult;
import com.willembergfilho.ifinance.domain.labor.PayslipLine;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NetSalaryResponse(
        int year,
        BigDecimal grossSalary,
        InssResult inss,
        IrrfResult irrf,
        BigDecimal alimony,
        BigDecimal otherDiscounts,
        BigDecimal totalDeductions,
        BigDecimal netSalary,
        BigDecimal fgts,
        BigDecimal totalTaxEffectiveRate,
        NetSalaryResult.EmployerCost employerCost,
        List<PayslipLine> lines
) {
    public static NetSalaryResponse from(NetSalaryResult r) {
        return new NetSalaryResponse(
                r.year(),
                r.grossSalary(),
                r.inss(),
                r.irrf(),
                r.alimony(),
                r.otherDiscounts(),
                r.totalDeductions(),
                r.netSalary(),
                r.fgts(),
                r.totalTaxEffectiveRate(),
                r.employerCost(),
                r.lines()
        );
    }
}
