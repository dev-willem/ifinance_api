package com.willembergfilho.ifinance.api.simulation;

import com.willembergfilho.ifinance.domain.simulation.CetCharge;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CetChargeRequest(
        @NotBlank String description,
        @NotNull CetCharge.ChargeType chargeType,
        @NotNull @Positive BigDecimal amount,
        Integer appliesOnPeriod
) {}
