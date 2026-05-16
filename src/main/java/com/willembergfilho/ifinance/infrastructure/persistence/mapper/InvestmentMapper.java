package com.willembergfilho.ifinance.infrastructure.persistence.mapper;

import com.willembergfilho.ifinance.domain.investment.*;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.InvestmentEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InvestmentMapper {

    private static BigDecimal strip(BigDecimal v) {
        return v == null ? null : new BigDecimal(v.stripTrailingZeros().toPlainString());
    }

    public InvestmentEntity toEntity(Investment investment) {
        InvestmentEntity entity = new InvestmentEntity();
        entity.setId(investment.getId());
        entity.setUserId(investment.getUserId());

        InvestmentParameters p = investment.getParameters();
        entity.setName(p.name());
        entity.setInvestmentType(p.type().name());
        entity.setRateBasis(p.rateBasis().name());
        entity.setRateValue(p.rateValue());
        entity.setPrincipal(p.principal());
        entity.setTermDays(p.termDays());
        entity.setStartDate(p.startDate());
        entity.setTaxExempt(p.type().isTaxExempt());
        entity.setStatus(investment.getStatus().name());

        InvestmentResult r = investment.getResult();
        if (r != null) {
            entity.setGrossReturn(r.grossReturn());
            entity.setNetReturn(r.netReturn());
            entity.setIrRate(r.irRate());
            entity.setIrAmount(r.irAmount());
            entity.setGrossAnnualRate(r.grossAnnualRate());
            entity.setNetAnnualRate(r.netAnnualRate());
            entity.setIndexRateUsed(r.indexRateUsed());
        }

        return entity;
    }

    public Investment toDomain(InvestmentEntity entity) {
        InvestmentParameters parameters = new InvestmentParameters(
                entity.getName(),
                InvestmentType.valueOf(entity.getInvestmentType()),
                RateBasis.valueOf(entity.getRateBasis()),
                strip(entity.getRateValue()),
                entity.getPrincipal(),
                entity.getTermDays(),
                entity.getStartDate()
        );

        InvestmentResult result = null;
        if (entity.getGrossReturn() != null) {
            result = new InvestmentResult(
                    entity.getGrossReturn(),
                    entity.getNetReturn(),
                    entity.getIrAmount(),
                    entity.getIrRate(),
                    entity.getGrossAnnualRate(),
                    entity.getNetAnnualRate(),
                    strip(entity.getIndexRateUsed())
            );
        }

        return new Investment(
                entity.getId(),
                entity.getUserId(),
                parameters,
                result,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
