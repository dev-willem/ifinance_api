package com.willembergfilho.ifinance.infrastructure.export;

import com.willembergfilho.ifinance.domain.math.AmortizationEngine;
import com.willembergfilho.ifinance.domain.simulation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class ExportFixtures {

    private ExportFixtures() {}

    static SimulationParameters priceParams(int term) {
        return new SimulationParameters("Financiamento Carro", AmortizationSystem.PRICE,
                new BigDecimal("10000.00"), new BigDecimal("1"), RateType.EFFECTIVE, term,
                Periodicity.MONTHLY, true, false, null, List.of());
    }

    // PRICE R$ 10.000 at 1% a.m. over 12 months: PMT = 888.49 (last one 888.47 absorbs rounding),
    // total paid 11 * 888.49 + 888.47 = 10661.86, total interest 661.86.
    static Simulation priceSimulation(int term) {
        List<Installment> installments = new AmortizationEngine()
                .calculatePrice(new BigDecimal("10000.00"), new BigDecimal("0.01"), term);
        CetResult cet = new CetResult(new BigDecimal("0.0156286319"), new BigDecimal("0.2045344583"), List.of());
        Instant now = Instant.now();
        return new Simulation(UUID.randomUUID(), UUID.randomUUID(), priceParams(term),
                AmortizationSchedule.of(installments, cet), SimulationStatus.CALCULATED, now, now);
    }
}
