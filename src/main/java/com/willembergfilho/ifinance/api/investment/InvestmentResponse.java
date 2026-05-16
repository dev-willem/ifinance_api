package com.willembergfilho.ifinance.api.investment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.investment.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InvestmentResponse(
        UUID id,
        String name,
        InvestmentType investmentType,
        RateBasis rateBasis,
        BigDecimal rateValue,
        BigDecimal principal,
        int termDays,
        LocalDate startDate,
        BigDecimal grossReturn,
        BigDecimal netReturn,
        BigDecimal irAmount,
        BigDecimal irRate,
        BigDecimal grossAnnualRate,
        BigDecimal netAnnualRate,
        BigDecimal indexRateUsed,
        boolean isTaxExempt,
        InvestmentStatus status,
        Instant createdAt
) {
    public static InvestmentResponse from(Investment investment) {
        InvestmentParameters p = investment.getParameters();
        InvestmentResult r = investment.getResult();

        BigDecimal irRatePct = null;
        BigDecimal grossAnnualRatePct = null;
        BigDecimal netAnnualRatePct = null;

        if (r != null) {
            irRatePct = r.irRate() != null
                    ? r.irRate().multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_EVEN)
                    : null;
            grossAnnualRatePct = r.grossAnnualRate() != null
                    ? r.grossAnnualRate().multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_EVEN)
                    : null;
            netAnnualRatePct = r.netAnnualRate() != null
                    ? r.netAnnualRate().multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_EVEN)
                    : null;
        }

        return new InvestmentResponse(
                investment.getId(),
                p.name(),
                p.type(),
                p.rateBasis(),
                p.rateValue(),
                p.principal(),
                p.termDays(),
                p.startDate(),
                r != null ? r.grossReturn() : null,
                r != null ? r.netReturn() : null,
                r != null ? r.irAmount() : null,
                irRatePct,
                grossAnnualRatePct,
                netAnnualRatePct,
                r != null ? r.indexRateUsed() : null,
                p.type().isTaxExempt(),
                investment.getStatus(),
                investment.getCreatedAt()
        );
    }
}
