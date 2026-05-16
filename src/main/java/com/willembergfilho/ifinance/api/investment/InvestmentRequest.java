package com.willembergfilho.ifinance.api.investment;

import com.willembergfilho.ifinance.domain.investment.InvestmentType;
import com.willembergfilho.ifinance.domain.investment.RateBasis;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentRequest(
        @NotBlank String name,
        @NotNull InvestmentType investmentType,
        @NotNull RateBasis rateBasis,
        @NotNull @Positive BigDecimal rateValue,
        @NotNull @Positive BigDecimal principal,
        @Min(1) @Max(3650) int termDays,
        @NotNull LocalDate startDate
) {}
