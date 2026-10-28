package com.willembergfilho.ifinance.api.tax;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.tax.SimplesAnnex;
import com.willembergfilho.ifinance.domain.tax.SimplesResult;
import com.willembergfilho.ifinance.domain.tax.TaxRules;
import com.willembergfilho.ifinance.domain.tax.TaxShare;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimplesResponse(
        SimplesAnnex requestedAnnex,
        SimplesAnnex appliedAnnex,
        BigDecimal fatorR,
        boolean eligible,
        boolean aboveSublimit,
        Integer bracket,
        BigDecimal nominalRate,
        BigDecimal deduction,
        BigDecimal effectiveRate,
        BigDecimal dasAmount,
        List<TaxShare> shares,
        List<String> notes,
        String disclaimer
) {
    public static SimplesResponse from(SimplesResult r) {
        return new SimplesResponse(
                r.requestedAnnex(),
                r.appliedAnnex(),
                r.fatorR(),
                r.eligible(),
                r.aboveSublimit(),
                r.bracket(),
                r.nominalRate(),
                r.deduction(),
                r.effectiveRate(),
                r.dasAmount(),
                r.shares(),
                r.notes(),
                TaxRules.DISCLAIMER
        );
    }
}
