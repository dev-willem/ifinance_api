package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.index.IndexRateRepository;
import com.willembergfilho.ifinance.domain.math.*;
import com.willembergfilho.ifinance.domain.simulation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RunSimulationUseCase {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SimulationRepository simulationRepository;
    private final IndexRateRepository indexRateRepository;
    private final InflationCorrectionEngine inflationCorrectionEngine;
    private final AmortizationEngine amortizationEngine;
    private final IrrCalculator irrCalculator;
    private final RateConverter rateConverter;

    public RunSimulationUseCase(SimulationRepository simulationRepository,
                                IndexRateRepository indexRateRepository) {
        this.simulationRepository = simulationRepository;
        this.indexRateRepository = indexRateRepository;
        this.inflationCorrectionEngine = new InflationCorrectionEngine();
        this.amortizationEngine = new AmortizationEngine();
        this.irrCalculator = new IrrCalculator();
        this.rateConverter = new RateConverter();
    }

    @Transactional
    public Simulation execute(UUID userId, SimulationParameters parameters) {
        log.info("Running simulation: userId={}, system={}, principal={}, term={}, periodicity={}",
                userId, parameters.amortizationSystem(), parameters.principal(),
                parameters.term(), parameters.periodicity());

        BigDecimal periodicRate = resolvePeriodicRate(parameters);

        List<Installment> installments = switch (parameters.amortizationSystem()) {
            case SAC     -> amortizationEngine.calculateSac(parameters.principal(), periodicRate, parameters.term());
            case PRICE   -> amortizationEngine.calculatePrice(parameters.principal(), periodicRate, parameters.term());
            case AMERICAN -> amortizationEngine.calculateAmerican(parameters.principal(), periodicRate, parameters.term());
            case GERMAN  -> amortizationEngine.calculateGerman(parameters.principal(), periodicRate, parameters.term());
            case SAM     -> amortizationEngine.calculateSam(parameters.principal(), periodicRate, parameters.term());
            default      -> throw new CalculationException(
                    "Amortization system not supported: " + parameters.amortizationSystem());
        };

        if (parameters.inflationCorrectionEnabled() && parameters.inflationIndex() != null) {
            List<BigDecimal> periodRates = fetchInflationRates(parameters);
            installments = inflationCorrectionEngine.applyCorrection(installments, periodicRate, periodRates);
        }

        CetResult cetResult = null;
        if (parameters.cetEnabled() && !parameters.charges().isEmpty()) {
            cetResult = calculateCet(parameters, installments);
        }

        AmortizationSchedule schedule = AmortizationSchedule.of(installments, cetResult);
        Simulation simulation = new Simulation(userId, parameters);
        simulation.applySchedule(schedule);
        Simulation saved = simulationRepository.save(simulation);
        log.info("Simulation completed: id={}, totalPaid={}", saved.getId(), schedule.totalPaid());
        return saved;
    }

    private List<BigDecimal> fetchInflationRates(SimulationParameters parameters) {
        // BCB rates come in percentage form (e.g. 0.67 = 0.67%), convert to decimal
        BigDecimal latestRate = indexRateRepository.findLatestByIndex(parameters.inflationIndex())
                .map(r -> MonetaryRounding.roundRate(r.rate().divide(HUNDRED, MonetaryRounding.MC)))
                .orElse(BigDecimal.ZERO);
        return Collections.nCopies(parameters.term(), latestRate);
    }

    private BigDecimal resolvePeriodicRate(SimulationParameters p) {
        BigDecimal rate = p.interestRate();
        // Accept percentage input (e.g. 11.75 for 11.75% a.a.) — normalize to decimal
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            rate = rate.divide(HUNDRED, MonetaryRounding.MC);
        }
        if (p.rateType() == RateType.EFFECTIVE) {
            return MonetaryRounding.roundRate(rate);
        }
        return rateConverter.toPeriodicRate(
                rateConverter.nominalToEffective(rate, p.periodicity().getPeriodsPerYear()),
                p.periodicity().getPeriodsPerYear()
        );
    }

    private CetResult calculateCet(SimulationParameters parameters, List<Installment> installments) {
        BigDecimal upfrontCharges = parameters.charges().stream()
                .filter(c -> c.appliesOnPeriod() != null && c.appliesOnPeriod() == 0)
                .map(c -> c.chargeType() == CetCharge.ChargeType.FIXED
                        ? c.amount()
                        : parameters.principal().multiply(c.amount(), MonetaryRounding.MC))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netReceived = parameters.principal().subtract(upfrontCharges, MonetaryRounding.MC);

        List<BigDecimal> cashFlows = new ArrayList<>();
        cashFlows.add(netReceived.negate());

        for (Installment inst : installments) {
            BigDecimal periodCharges = parameters.charges().stream()
                    .filter(c -> c.appliesOnPeriod() == null)
                    .map(c -> c.chargeType() == CetCharge.ChargeType.FIXED
                            ? c.amount()
                            : inst.principalBalanceBefore().multiply(c.amount(), MonetaryRounding.MC))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            cashFlows.add(inst.total().add(periodCharges, MonetaryRounding.MC));
        }

        BigDecimal monthlyRate = irrCalculator.calculate(cashFlows);
        BigDecimal annualRate = rateConverter.toEffectiveAnnual(monthlyRate,
                parameters.periodicity().getPeriodsPerYear());
        return new CetResult(monthlyRate, annualRate, cashFlows);
    }
}
