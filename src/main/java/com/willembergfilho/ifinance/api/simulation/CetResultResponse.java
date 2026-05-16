package com.willembergfilho.ifinance.api.simulation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.simulation.CetResult;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CetResultResponse(BigDecimal monthlyRate, BigDecimal annualRate) {

    public static CetResultResponse from(CetResult result) {
        if (result == null) return null;
        return new CetResultResponse(result.monthlyRate(), result.annualRate());
    }
}
