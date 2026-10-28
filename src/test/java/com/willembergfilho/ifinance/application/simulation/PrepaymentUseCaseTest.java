package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.math.AmortizationEngine;
import com.willembergfilho.ifinance.domain.simulation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrepaymentUseCaseTest {

    private static final UUID USER = UUID.randomUUID();

    private SimulationRepository repo;
    private PrepaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = mock(SimulationRepository.class);
        useCase = new PrepaymentUseCase(repo);
    }

    // PRICE 1000 at 1% a.m., 3x: PMT 340.02
    // P1: J 10.00, A 330.02, SD 669.98 | P2: J 6.70, A 333.32, SD 336.66 | P3: J 3.37, A 336.66, PMT 340.03
    private Simulation price1000(String rate, RateType type) {
        List<Installment> installments = new AmortizationEngine()
                .calculatePrice(new BigDecimal("1000.00"), new BigDecimal("0.01"), 3);
        SimulationParameters params = new SimulationParameters("p", AmortizationSystem.PRICE, new BigDecimal("1000.00"),
                new BigDecimal(rate), type, 3, Periodicity.MONTHLY, false, false, null, List.of());
        Simulation s = new Simulation(UUID.randomUUID(), USER, params, AmortizationSchedule.of(installments, null),
                SimulationStatus.CALCULATED, Instant.now(), Instant.now());
        when(repo.findById(s.getId())).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void prepaymentAtContractedRateEqualsOutstandingBalance() {
        // at period 2: outstanding 669.98; PV = 340.02/1.01 + 340.03/1.0201 = 336.6535 + 333.3301 = 669.98
        Simulation s = price1000("1", RateType.EFFECTIVE);

        PrepaymentResult r = useCase.execute(USER, s.getId(), 2);

        assertThat(r.atPeriod()).isEqualTo(2);
        assertThat(r.remainingPeriods()).isEqualTo(2);
        assertThat(r.outstandingBalance()).isEqualByComparingTo("669.98");
        assertThat(r.prepaymentValue()).isEqualByComparingTo("669.98");
        assertThat(r.discountAmount()).isEqualByComparingTo("0");
        assertThat(r.discountPercent()).isEqualByComparingTo("0");
        assertThat(r.contractedPeriodicRatePercent()).isEqualByComparingTo("1.00");
    }

    @Test
    void prepaymentAtFirstPeriodCoversWholeSchedule() {
        // 340.02/1.01 + 340.02/1.0201 + 340.03/1.030301 = 336.6535 + 333.3203 + 330.0298 = 1000.0036 -> 1000.00
        Simulation s = price1000("1", RateType.EFFECTIVE);

        PrepaymentResult r = useCase.execute(USER, s.getId(), 1);

        assertThat(r.remainingPeriods()).isEqualTo(3);
        assertThat(r.outstandingBalance()).isEqualByComparingTo("1000.00");
        assertThat(r.prepaymentValue()).isEqualByComparingTo("1000.00");
        assertThat(r.discountAmount()).isEqualByComparingTo("0");   // never negative
    }

    @Test
    void nominalAnnualRateIsConvertedToMonthly() {
        // 12% a.a. nominal, monthly capitalization -> 1% a.m.
        Simulation s = price1000("12", RateType.NOMINAL);

        assertThat(useCase.execute(USER, s.getId(), 3).contractedPeriodicRatePercent()).isEqualByComparingTo("1.00");
    }

    @Test
    void periodOutOfRangeIsRejected() {
        Simulation s = price1000("1", RateType.EFFECTIVE);

        assertThatThrownBy(() -> useCase.execute(USER, s.getId(), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(USER, s.getId(), 4)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void foreignSimulationIsNotFound() {
        Simulation s = price1000("1", RateType.EFFECTIVE);

        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), s.getId(), 1))
                .isInstanceOf(SimulationNotFoundException.class);
    }
}
