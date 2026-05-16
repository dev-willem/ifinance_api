package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.math.MonetaryRounding;
import com.willembergfilho.ifinance.domain.math.RateConverter;
import com.willembergfilho.ifinance.domain.simulation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;

@Slf4j
@Service
public class PrepaymentUseCase {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SimulationRepository simulationRepository;
    private final RateConverter rateConverter;

    public PrepaymentUseCase(SimulationRepository simulationRepository) {
        this.simulationRepository = simulationRepository;
        this.rateConverter = new RateConverter();
    }

    @Transactional(readOnly = true)
    public PrepaymentResult execute(UUID userId, UUID simulationId, int atPeriod) {
        Simulation simulation = simulationRepository.findById(simulationId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new SimulationNotFoundException(simulationId));

        List<Installment> installments = simulation.getSchedule().installments();
        int term = installments.size();

        if (atPeriod < 1 || atPeriod > term) {
            throw new IllegalArgumentException(
                    "Period %d is out of range [1, %d]".formatted(atPeriod, term));
        }

        BigDecimal periodicRate = resolvePeriodicRate(simulation.getParameters());

        // Remaining installments: atPeriod (inclusive) to n
        List<Installment> remaining = installments.subList(atPeriod - 1, term);
        BigDecimal outstandingBalance = remaining.get(0).principalBalanceBefore();

        // PV of remaining installments discounted at the contracted periodic rate
        BigDecimal pv = BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE;
        BigDecimal factorMult = BigDecimal.ONE.add(periodicRate, MC);
        for (Installment inst : remaining) {
            factor = factor.multiply(factorMult, MC);
            pv = pv.add(inst.total().divide(factor, MC), MC);
        }
        pv = MonetaryRounding.round(pv);

        BigDecimal discountAmount = MonetaryRounding.round(
                outstandingBalance.subtract(pv, MC).max(BigDecimal.ZERO));

        BigDecimal discountPercent = outstandingBalance.compareTo(BigDecimal.ZERO) > 0
                ? MonetaryRounding.round(
                        discountAmount.multiply(HUNDRED, MC).divide(outstandingBalance, MC))
                : BigDecimal.ZERO;

        BigDecimal contractedRatePercent = MonetaryRounding.round(periodicRate.multiply(HUNDRED, MC));

        log.info("Prepayment: simulationId={}, atPeriod={}, outstanding={}, pv={}, discount={}",
                simulationId, atPeriod, outstandingBalance, pv, discountAmount);

        return new PrepaymentResult(
                atPeriod,
                remaining.size(),
                outstandingBalance,
                pv,
                discountAmount,
                discountPercent,
                contractedRatePercent
        );
    }

    private BigDecimal resolvePeriodicRate(SimulationParameters p) {
        BigDecimal rate = p.interestRate();
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            rate = rate.divide(HUNDRED, MC);
        }
        if (p.rateType() == RateType.EFFECTIVE) {
            return MonetaryRounding.roundRate(rate);
        }
        return rateConverter.toPeriodicRate(
                rateConverter.nominalToEffective(rate, p.periodicity().getPeriodsPerYear()),
                p.periodicity().getPeriodsPerYear()
        );
    }
}
