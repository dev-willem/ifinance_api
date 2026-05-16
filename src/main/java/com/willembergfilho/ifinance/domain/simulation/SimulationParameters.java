package com.willembergfilho.ifinance.domain.simulation;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;

import java.math.BigDecimal;
import java.util.List;

public record SimulationParameters(
        String name,
        AmortizationSystem amortizationSystem,
        BigDecimal principal,
        BigDecimal interestRate,
        RateType rateType,
        int term,
        Periodicity periodicity,
        boolean cetEnabled,
        boolean inflationCorrectionEnabled,
        EconomicIndex inflationIndex,
        List<CetCharge> charges
) {}
