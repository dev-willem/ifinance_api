package com.willembergfilho.ifinance.api.credit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.credit.DiscountModality;
import com.willembergfilho.ifinance.domain.credit.DiscountResult;

import java.math.BigDecimal;
import java.util.List;

public record DiscountResponse(
        BigDecimal faceValue,
        BigDecimal ratePerPeriod,
        BigDecimal periods,
        List<Modality> modalities
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Modality(
            DiscountModality modality,
            boolean valid,
            BigDecimal presentValue,
            BigDecimal discountAmount,
            BigDecimal effectiveRateTotal,
            BigDecimal effectiveSimpleRatePerPeriod,
            BigDecimal effectiveCompoundRatePerPeriod,
            String message
    ) {
        static Modality from(DiscountResult r) {
            return new Modality(r.modality(), r.valid(), r.presentValue(), r.discountAmount(),
                    r.effectiveRateTotal(), r.effectiveSimpleRatePerPeriod(),
                    r.effectiveCompoundRatePerPeriod(), r.message());
        }
    }

    public static DiscountResponse from(BigDecimal faceValue, BigDecimal ratePerPeriod, BigDecimal periods,
                                        List<DiscountResult> results) {
        return new DiscountResponse(faceValue, ratePerPeriod, periods,
                results.stream().map(Modality::from).toList());
    }
}
