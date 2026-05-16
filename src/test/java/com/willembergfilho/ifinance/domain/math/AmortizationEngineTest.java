package com.willembergfilho.ifinance.domain.math;

import com.willembergfilho.ifinance.domain.simulation.Installment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AmortizationEngineTest {

    private AmortizationEngine engine;

    private static final BigDecimal PRINCIPAL = new BigDecimal("100000.00");
    private static final BigDecimal RATE_1PCT = new BigDecimal("0.01");

    @BeforeEach
    void setUp() {
        engine = new AmortizationEngine();
    }

    // ------------------------------------------------------------------ SAC --

    @Test
    void sac_3months_amortizationIsConstant() {
        List<Installment> schedule = engine.calculateSac(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(3, schedule.size());
        assertEquals(new BigDecimal("33333.33"), schedule.get(0).amortization());
        assertEquals(new BigDecimal("33333.33"), schedule.get(1).amortization());
    }

    @Test
    void sac_3months_firstPeriodInterest() {
        List<Installment> schedule = engine.calculateSac(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(new BigDecimal("1000.00"), schedule.getFirst().interest());
    }

    @Test
    void sac_3months_lastBalanceIsZero() {
        List<Installment> schedule = engine.calculateSac(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(BigDecimal.ZERO.setScale(2), schedule.getLast().principalBalanceAfter());
    }

    @Test
    void sac_120months_totalPaid() {
        List<Installment> schedule = engine.calculateSac(PRINCIPAL, RATE_1PCT, 120);

        BigDecimal totalPaid = schedule.stream()
                .map(Installment::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("160500.24"), totalPaid);
    }

    @Test
    void sac_lastPeriodBalanceAfterIsZero() {
        List<Installment> schedule = engine.calculateSac(PRINCIPAL, RATE_1PCT, 120);

        assertEquals(BigDecimal.ZERO.setScale(2), schedule.getLast().principalBalanceAfter());
    }

    // ---------------------------------------------------------------- PRICE --

    @Test
    void price_3months_allInstallmentsEqual() {
        List<Installment> schedule = engine.calculatePrice(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(3, schedule.size());
        BigDecimal first = schedule.get(0).total();
        BigDecimal second = schedule.get(1).total();
        // Last period corrects rounding so it may differ by at most 1 cent
        assertEquals(first, second);
        assertTrue(first.compareTo(new BigDecimal("34000.00")) > 0);
        assertTrue(first.compareTo(new BigDecimal("35000.00")) < 0);
    }

    @Test
    void price_3months_lastBalanceIsZero() {
        List<Installment> schedule = engine.calculatePrice(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(BigDecimal.ZERO.setScale(2), schedule.getLast().principalBalanceAfter());
    }

    @Test
    void price_3months_firstInterest() {
        List<Installment> schedule = engine.calculatePrice(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(new BigDecimal("1000.00"), schedule.getFirst().interest());
    }

    @Test
    void price_120months_lastBalanceIsZero() {
        List<Installment> schedule = engine.calculatePrice(PRINCIPAL, RATE_1PCT, 120);

        assertEquals(BigDecimal.ZERO.setScale(2), schedule.getLast().principalBalanceAfter());
    }

    // -------------------------------------------------------------- AMERICAN --

    @Test
    void american_3months_interestOnlyPeriodsBeforeLast() {
        List<Installment> schedule = engine.calculateAmerican(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(3, schedule.size());
        assertEquals(new BigDecimal("1000.00"), schedule.get(0).total());
        assertEquals(new BigDecimal("1000.00"), schedule.get(1).total());
    }

    @Test
    void american_3months_amortizationZeroUntilLastPeriod() {
        List<Installment> schedule = engine.calculateAmerican(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(BigDecimal.ZERO, schedule.get(0).amortization());
        assertEquals(BigDecimal.ZERO, schedule.get(1).amortization());
    }

    @Test
    void american_3months_lastPeriodPrincipalPlusInterest() {
        List<Installment> schedule = engine.calculateAmerican(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(new BigDecimal("101000.00"), schedule.getLast().total());
        assertEquals(PRINCIPAL, schedule.getLast().amortization());
    }

    @Test
    void american_3months_lastBalanceIsZero() {
        List<Installment> schedule = engine.calculateAmerican(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(BigDecimal.ZERO.setScale(2), schedule.getLast().principalBalanceAfter());
    }

    // --------------------------------------------------------------- GERMAN --

    @Test
    void german_3months_constantTotalPayment() {
        List<Installment> schedule = engine.calculateGerman(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(3, schedule.size());
        BigDecimal expected = new BigDecimal("34333.33");
        assertEquals(expected, schedule.get(0).total());
        assertEquals(expected, schedule.get(1).total());
    }

    @Test
    void german_3months_constantInterestOnOriginalPrincipal() {
        List<Installment> schedule = engine.calculateGerman(PRINCIPAL, RATE_1PCT, 3);

        BigDecimal expectedInterest = new BigDecimal("1000.00");
        assertEquals(expectedInterest, schedule.get(0).interest());
        assertEquals(expectedInterest, schedule.get(1).interest());
        assertEquals(expectedInterest, schedule.get(2).interest());
    }

    @Test
    void german_3months_lastBalanceIsZero() {
        List<Installment> schedule = engine.calculateGerman(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(BigDecimal.ZERO.setScale(2), schedule.getLast().principalBalanceAfter());
    }

    // ------------------------------------------------------------------- SAM --

    @Test
    void sam_3months_installmentIsMidpointOfSacAndPrice() {
        List<Installment> sacSchedule = engine.calculateSac(PRINCIPAL, RATE_1PCT, 3);
        List<Installment> priceSchedule = engine.calculatePrice(PRINCIPAL, RATE_1PCT, 3);
        List<Installment> samSchedule = engine.calculateSam(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(3, samSchedule.size());

        BigDecimal priceInstallment = priceSchedule.get(0).total();

        for (int i = 0; i < 2; i++) {
            BigDecimal sacTotal = sacSchedule.get(i).total();
            BigDecimal expected = sacTotal.add(priceInstallment)
                    .divide(new BigDecimal("2"), MonetaryRounding.MONETARY_SCALE,
                            MonetaryRounding.MODE);
            assertEquals(expected, samSchedule.get(i).total(),
                    "SAM installment mismatch at period " + (i + 1));
        }
    }

    @Test
    void sam_3months_lastBalanceIsZero() {
        List<Installment> schedule = engine.calculateSam(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(BigDecimal.ZERO.setScale(2), schedule.getLast().principalBalanceAfter());
    }

    @Test
    void sam_3months_periodCount() {
        List<Installment> schedule = engine.calculateSam(PRINCIPAL, RATE_1PCT, 3);

        assertEquals(3, schedule.size());
    }

    // ----------------------------------------------------------- VALIDATION --

    @Test
    void validate_negativePrincipalThrows() {
        assertThrows(CalculationException.class,
                () -> engine.calculateSac(new BigDecimal("-1000"), RATE_1PCT, 12));
    }

    @Test
    void validate_negativeRateThrows() {
        assertThrows(CalculationException.class,
                () -> engine.calculatePrice(PRINCIPAL, new BigDecimal("-0.01"), 12));
    }

    @Test
    void validate_zeroTermsThrows() {
        assertThrows(CalculationException.class,
                () -> engine.calculateAmerican(PRINCIPAL, RATE_1PCT, 0));
    }
}
