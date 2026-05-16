package com.willembergfilho.ifinance.api.analysis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.application.analysis.FinancialAnalysisUseCase;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalysisResponse(
        BigDecimal npv,
        boolean npvPositive,
        BigDecimal irrPercent,
        BigDecimal irrDecimal,
        boolean paybackAchieved,
        BigDecimal simplePayback,
        Integer simplePaybackPeriod,
        BigDecimal discountedPayback,
        Integer discountedPaybackPeriod
) {
    public static AnalysisResponse from(FinancialAnalysisUseCase.AnalysisResult result) {
        return new AnalysisResponse(
                result.npv(),
                result.npvPositive(),
                result.irrPercent(),
                result.irrDecimal(),
                result.paybackAchieved(),
                result.simplePayback(),
                result.simplePaybackPeriod(),
                result.discountedPayback(),
                result.discountedPaybackPeriod()
        );
    }
}
