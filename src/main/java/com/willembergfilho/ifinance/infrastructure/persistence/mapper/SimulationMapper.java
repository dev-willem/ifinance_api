package com.willembergfilho.ifinance.infrastructure.persistence.mapper;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.simulation.*;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.InstallmentEntity;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.SimulationEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SimulationMapper {

    private static BigDecimal strip(BigDecimal v) {
        return v == null ? null : new BigDecimal(v.stripTrailingZeros().toPlainString());
    }

    public SimulationEntity toEntity(Simulation simulation) {
        SimulationEntity entity = new SimulationEntity();
        entity.setId(simulation.getId());
        entity.setUserId(simulation.getUserId());

        SimulationParameters p = simulation.getParameters();
        entity.setName(p.name());
        entity.setAmortizationSystem(p.amortizationSystem().name());
        entity.setPrincipal(p.principal());
        entity.setInterestRate(p.interestRate());
        entity.setRateType(p.rateType().name());
        entity.setTerm(p.term());
        entity.setPeriodicity(p.periodicity().name());
        entity.setCetEnabled(p.cetEnabled());
        entity.setInflationCorrectionEnabled(p.inflationCorrectionEnabled());
        entity.setInflationIndex(p.inflationIndex() != null ? p.inflationIndex().name() : null);
        entity.setStatus(simulation.getStatus().name());

        if (simulation.getSchedule() != null) {
            entity.setTotalPaid(simulation.getSchedule().totalPaid());
            if (simulation.getSchedule().cetResult() != null) {
                entity.setEffectiveCostRate(simulation.getSchedule().cetResult().annualRate());
            }
            List<InstallmentEntity> installmentEntities = simulation.getSchedule().installments().stream()
                    .map(i -> toInstallmentEntity(i, entity))
                    .toList();
            entity.setInstallments(installmentEntities);
        }

        return entity;
    }

    public Simulation toDomain(SimulationEntity entity) {
        EconomicIndex inflationIndex = entity.getInflationIndex() != null
                ? EconomicIndex.valueOf(entity.getInflationIndex())
                : null;

        SimulationParameters parameters = new SimulationParameters(
                entity.getName(),
                AmortizationSystem.valueOf(entity.getAmortizationSystem()),
                entity.getPrincipal(),
                strip(entity.getInterestRate()),
                RateType.valueOf(entity.getRateType()),
                entity.getTerm(),
                Periodicity.valueOf(entity.getPeriodicity()),
                entity.isCetEnabled(),
                entity.isInflationCorrectionEnabled(),
                inflationIndex,
                List.of()
        );

        AmortizationSchedule schedule = null;
        if (!entity.getInstallments().isEmpty()) {
            List<Installment> installments = entity.getInstallments().stream()
                    .map(this::toInstallmentDomain)
                    .toList();
            CetResult cetResult = null;
            if (entity.getEffectiveCostRate() != null) {
                BigDecimal annualRate = entity.getEffectiveCostRate();
                BigDecimal monthlyRate = new BigDecimal(
                        String.valueOf(Math.pow(annualRate.add(BigDecimal.ONE).doubleValue(), 1.0 / 12) - 1));
                cetResult = new CetResult(monthlyRate, annualRate, List.of());
            }
            schedule = AmortizationSchedule.of(installments, cetResult);
        }

        return new Simulation(
                entity.getId(),
                entity.getUserId(),
                parameters,
                schedule,
                SimulationStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private InstallmentEntity toInstallmentEntity(Installment installment, SimulationEntity simulation) {
        InstallmentEntity entity = new InstallmentEntity();
        entity.setSimulation(simulation);
        entity.setPeriodNumber(installment.periodNumber());
        entity.setPrincipalBalanceBefore(installment.principalBalanceBefore());
        entity.setAmortization(installment.amortization());
        entity.setInterest(installment.interest());
        entity.setAdditionalCharges(installment.additionalCharges());
        entity.setTotal(installment.total());
        entity.setPrincipalBalanceAfter(installment.principalBalanceAfter());
        return entity;
    }

    private Installment toInstallmentDomain(InstallmentEntity entity) {
        return new Installment(
                entity.getPeriodNumber(),
                entity.getPrincipalBalanceBefore(),
                entity.getAmortization(),
                entity.getInterest(),
                entity.getAdditionalCharges(),
                entity.getTotal(),
                entity.getPrincipalBalanceAfter()
        );
    }
}
