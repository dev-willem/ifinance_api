package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.index.IndexRateRepository;
import com.willembergfilho.ifinance.domain.simulation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RunSimulationUseCaseTest {

    private RunSimulationUseCase useCase;

    @BeforeEach
    void setUp() {
        SimulationRepository repo = mock(SimulationRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        useCase = new RunSimulationUseCase(repo, mock(IndexRateRepository.class));
    }

    private CetResult cetFor(CetCharge charge) {
        SimulationParameters params = new SimulationParameters("x", AmortizationSystem.SAC,
                new BigDecimal("300000"), new BigDecimal("12"), RateType.NOMINAL, 120,
                Periodicity.MONTHLY, true, false, null, List.of(charge));
        return useCase.execute(UUID.randomUUID(), params).getSchedule().cetResult();
    }

    @Test
    void percentageUpfrontChargeIsTreatedAsPercent() {
        // 1% of 300000 = 3000 upfront fee
        CetResult cet = cetFor(new CetCharge("T", CetCharge.ChargeType.PERCENTAGE, BigDecimal.ONE, 0));
        assertEquals(0.01024, cet.monthlyRate().doubleValue(), 0.0001);
        assertEquals(0.13005, cet.annualRate().doubleValue(), 0.001);
    }

    @Test
    void percentageAndFixedUpfrontChargesAreEquivalent() {
        CetResult pct = cetFor(new CetCharge("P", CetCharge.ChargeType.PERCENTAGE, BigDecimal.ONE, 0));
        CetResult fixed = cetFor(new CetCharge("F", CetCharge.ChargeType.FIXED, new BigDecimal("3000"), 0));
        assertEquals(fixed.monthlyRate().doubleValue(), pct.monthlyRate().doubleValue(), 1e-8);
    }

    @Test
    void percentagePerPeriodChargeIsTreatedAsPercent() {
        // 0.01% of balance per installment: CET only slightly above the nominal-derived rate (1% a.m.)
        CetResult cet = cetFor(new CetCharge("S", CetCharge.ChargeType.PERCENTAGE, new BigDecimal("0.01"), null));
        assertTrue(cet.monthlyRate().doubleValue() > 0.0100 && cet.monthlyRate().doubleValue() < 0.0115);
    }
}
