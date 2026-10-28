package com.willembergfilho.ifinance.application.planning;

import com.willembergfilho.ifinance.domain.planning.EmergencyReserveParameters;
import com.willembergfilho.ifinance.domain.planning.EmergencyReserveResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class EmergencyReserveUseCaseTest {

    private final EmergencyReserveUseCase useCase = new EmergencyReserveUseCase();

    private static EmergencyReserveParameters params(String expenses, int months, String current, String monthly,
                                                     String annualReturn) {
        return new EmergencyReserveParameters(new BigDecimal(expenses), months, new BigDecimal(current),
                new BigDecimal(monthly), annualReturn == null ? null : new BigDecimal(annualReturn));
    }

    @Test
    void progressAndLinearTimeWithoutReturn() {
        // target 5000 * 6 = 30000; coverage 10000/5000 = 2 months; progress 33.33%; (30000-10000)/2000 = 10 months
        EmergencyReserveResult r = useCase.execute(params("5000", 6, "10000", "2000", null));

        assertThat(r.targetAmount()).isEqualByComparingTo("30000.00");
        assertThat(r.currentCoverageMonths()).isEqualTo(2.0);
        assertThat(r.progressPercent()).isCloseTo(33.333, within(0.001));
        assertThat(r.targetReached()).isFalse();
        assertThat(r.monthsToComplete()).isEqualTo(10);
        assertThat(r.monthlyShortfall()).isEqualByComparingTo("20000.00");
    }

    @Test
    void returnShortensTimeToTarget() {
        // without return: 30000/500 = 60 months
        // with 12% a.a.: r = 1.12^(1/12) - 1 = 0.0094888; n = ln((30000r + 500)/500)/ln(1+r) = 47.72 -> 48
        assertThat(useCase.execute(params("5000", 6, "0", "500", null)).monthsToComplete()).isEqualTo(60);
        assertThat(useCase.execute(params("5000", 6, "0", "500", "0.12")).monthsToComplete()).isEqualTo(48);
    }

    @Test
    void targetReachedCapsProgress() {
        EmergencyReserveResult r = useCase.execute(params("5000", 6, "40000", "0", null));

        assertThat(r.targetReached()).isTrue();
        assertThat(r.progressPercent()).isEqualTo(100.0);
        assertThat(r.currentCoverageMonths()).isEqualTo(8.0);
        assertThat(r.monthsToComplete()).isNull();
        assertThat(r.monthlyShortfall()).isNull();
    }

    @Test
    void neverReachedWithoutSavings() {
        EmergencyReserveResult r = useCase.execute(params("5000", 6, "1000", "0", "0"));

        assertThat(r.targetReached()).isFalse();
        assertThat(r.monthsToComplete()).isNull();
    }
}
