package com.willembergfilho.ifinance.api.simulation;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.simulation.AmortizationSystem;
import com.willembergfilho.ifinance.domain.simulation.Periodicity;
import com.willembergfilho.ifinance.domain.simulation.RateType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record SimulationRequest(
        @NotBlank String name,

        @NotNull AmortizationSystem amortizationSystem,

        @NotNull @Positive BigDecimal principal,

        @NotNull @DecimalMin("0.0") BigDecimal interestRate,

        @NotNull RateType rateType,

        @Min(1) @Max(600) int term,

        @NotNull Periodicity periodicity,

        boolean cetEnabled,

        boolean inflationCorrectionEnabled,

        EconomicIndex inflationIndex,

        @Valid List<CetChargeRequest> charges
) {}
