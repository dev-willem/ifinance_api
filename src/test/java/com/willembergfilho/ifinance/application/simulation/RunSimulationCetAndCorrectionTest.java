package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.index.IndexRate;
import com.willembergfilho.ifinance.domain.index.IndexRateRepository;
import com.willembergfilho.ifinance.domain.math.CalculationException;
import com.willembergfilho.ifinance.domain.math.NpvCalculator;
import com.willembergfilho.ifinance.domain.simulation.*;
import com.willembergfilho.ifinance.domain.simulation.CetCharge.ChargeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RunSimulationCetAndCorrectionTest {

    private SimulationRepository simulations;
    private IndexRateRepository indexes;
    private RunSimulationUseCase useCase;

    @BeforeEach
    void setUp() {
        simulations = mock(SimulationRepository.class);
        indexes = mock(IndexRateRepository.class);
        when(simulations.save(any())).thenAnswer(i -> i.getArgument(0));
        useCase = new RunSimulationUseCase(simulations, indexes);
    }

    private static SimulationParameters params(AmortizationSystem system, String principal, int term,
                                               boolean cet, boolean correction, EconomicIndex index,
                                               List<CetCharge> charges) {
        return new SimulationParameters("t", system, new BigDecimal(principal), new BigDecimal("1"),
                RateType.EFFECTIVE, term, Periodicity.MONTHLY, cet, correction, index, charges);
    }

    private static final List<CetCharge> TYPICAL_CHARGES = List.of(
            new CetCharge("TAC", ChargeType.FIXED, new BigDecimal("200"), 0),
            new CetCharge("IOF adicional", ChargeType.PERCENTAGE, new BigDecimal("0.38"), 0),
            new CetCharge("Seguro prestamista", ChargeType.FIXED, new BigDecimal("10"), null));

    // ------------------------------------------------------------------ CET --

    @Test
    void cetWithFeesIofAndInsuranceExceedsContractRate() {
        // PRICE 10000, 1% a.m., 12x -> PMT 888.49 (last 888.47).
        // Released: 10000 - 200 (TAC) - 38 (0.38% IOF) = 9762; each payment + 10 insurance.
        // IRR of [-9762, 898.49 x11, 898.47] = 1.5629% a.m. -> (1.015629)^12 - 1 = 20.45% a.a.
        Simulation s = useCase.execute(UUID.randomUUID(),
                params(AmortizationSystem.PRICE, "10000.00", 12, true, false, null, TYPICAL_CHARGES));
        CetResult cet = s.getSchedule().cetResult();

        assertThat(cet.cashFlows().getFirst()).isEqualByComparingTo("-9762");
        assertThat(cet.cashFlows().get(1)).isEqualByComparingTo("898.49");
        assertThat(cet.cashFlows().getLast()).isEqualByComparingTo("898.47");
        assertThat(cet.monthlyRate().doubleValue()).isCloseTo(0.0156286, within(1e-6));
        assertThat(cet.annualRate().doubleValue()).isCloseTo(0.204534, within(1e-5));
        assertThat(cet.monthlyRate()).isGreaterThan(new BigDecimal("0.01"));
        // NPV of the cash flows at the CET is zero
        assertThat(new NpvCalculator().calculate(cet.cashFlows(), cet.monthlyRate()).abs())
                .isLessThan(new BigDecimal("0.01"));
    }

    @Test
    void cetWithoutChargesIsNotComputed() {
        Simulation s = useCase.execute(UUID.randomUUID(),
                params(AmortizationSystem.SAC, "10000.00", 12, true, false, null, List.of()));

        assertThat(s.getSchedule().cetResult()).isNull();
    }

    @Test
    void chargesAreIgnoredWhenCetDisabled() {
        Simulation s = useCase.execute(UUID.randomUUID(),
                params(AmortizationSystem.SAC, "10000.00", 12, false, false, null, TYPICAL_CHARGES));

        assertThat(s.getSchedule().cetResult()).isNull();
    }

    @Test
    void chargeOnSpecificPeriodEntersCetOnlyOnThatPeriod() {
        // CMN Res. 3.517/2007: CET must include every charge; a R$ 500 fee due with installment 1
        Simulation s = useCase.execute(UUID.randomUUID(), params(AmortizationSystem.PRICE, "10000.00", 12, true, false, null,
                List.of(new CetCharge("Tarifa de avaliação", ChargeType.FIXED, new BigDecimal("500"), 1))));
        CetResult cet = s.getSchedule().cetResult();

        assertThat(cet.cashFlows().getFirst()).isEqualByComparingTo("-10000");
        assertThat(cet.cashFlows().get(1)).isEqualByComparingTo("1388.49");
        assertThat(cet.cashFlows().get(2)).isEqualByComparingTo("888.49");
        assertThat(cet.monthlyRate()).isGreaterThan(new BigDecimal("0.01"));
    }

    @Test
    void cetEnabledWithNullChargesDoesNotFail() {
        assertThatCode(() -> useCase.execute(UUID.randomUUID(),
                params(AmortizationSystem.SAC, "10000.00", 12, true, false, null, null)))
                .doesNotThrowAnyException();
    }

    // ---------------------------------------------------- monetary correction --

    @Test
    void monetaryCorrectionWithLatestIpca() {
        // SAC 1200, 1% a.m., 3x, IPCA 0.50% a.m.:
        // P1: CM 6.00 -> 1206.00; J 12.06; A 402.00; PMT 414.06
        // P2: CM 4.02 -> 808.02; J 8.08; A 404.01; PMT 412.09
        // P3: CM 2.02 -> 406.03; J 4.06; A 406.03; PMT 410.09   => total 1236.24, interest 24.20
        when(indexes.findLatestByIndex(EconomicIndex.IPCA)).thenReturn(Optional.of(
                new IndexRate(EconomicIndex.IPCA, LocalDate.of(2026, 9, 1), new BigDecimal("0.50"), false)));

        Simulation s = useCase.execute(UUID.randomUUID(),
                params(AmortizationSystem.SAC, "1200.00", 3, false, true, EconomicIndex.IPCA, List.of()));
        AmortizationSchedule schedule = s.getSchedule();

        assertThat(schedule.installments()).extracting(Installment::additionalCharges)
                .containsExactly(new BigDecimal("6.00"), new BigDecimal("4.02"), new BigDecimal("2.02"));
        assertThat(schedule.installments()).extracting(Installment::total)
                .containsExactly(new BigDecimal("414.06"), new BigDecimal("412.09"), new BigDecimal("410.09"));
        assertThat(schedule.totalPaid()).isEqualByComparingTo("1236.24");
        assertThat(schedule.totalInterest()).isEqualByComparingTo("24.20");
        assertThat(s.getStatus()).isEqualTo(SimulationStatus.CALCULATED);
    }

    @Test
    void monetaryCorrectionWithoutIndexDataFails() {
        when(indexes.findLatestByIndex(EconomicIndex.IGP_M)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(),
                params(AmortizationSystem.SAC, "1200.00", 3, false, true, EconomicIndex.IGP_M, List.of())))
                .isInstanceOf(CalculationException.class);
        verify(simulations, never()).save(any());
    }

    @Test
    void correctionFlagWithoutIndexKeepsPlainSchedule() {
        Simulation s = useCase.execute(UUID.randomUUID(),
                params(AmortizationSystem.SAC, "1200.00", 3, false, true, null, List.of()));

        // plain SAC: 400 + 12 / 400 + 8 / 400 + 4
        assertThat(s.getSchedule().totalPaid()).isEqualByComparingTo("1224.00");
        verifyNoInteractions(indexes);
    }

    @Disabled("Bug (needs design decision): InflationCorrectionEngine only uses the base schedule's size and opening "
            + "balance and always amortizes correctedBalance/remaining, so PRICE/AMERICAN/GERMAN/SAM become SAC once "
            + "correction is enabled - even with a 0% index. PRICE with correction should keep constant real installments.")
    @Test
    void priceWithZeroInflationKeepsConstantInstallments() {
        when(indexes.findLatestByIndex(EconomicIndex.TR)).thenReturn(Optional.of(
                new IndexRate(EconomicIndex.TR, LocalDate.of(2026, 9, 1), BigDecimal.ZERO, false)));

        Simulation s = useCase.execute(UUID.randomUUID(),
                params(AmortizationSystem.PRICE, "1000.00", 3, false, true, EconomicIndex.TR, List.of()));

        assertThat(s.getSchedule().installments().getFirst().total()).isEqualByComparingTo("340.02");
    }
}
