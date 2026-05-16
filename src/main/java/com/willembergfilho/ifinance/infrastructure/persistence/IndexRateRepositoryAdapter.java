package com.willembergfilho.ifinance.infrastructure.persistence;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.index.IndexRate;
import com.willembergfilho.ifinance.domain.index.IndexRateRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.IndexRateEntity;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class IndexRateRepositoryAdapter implements IndexRateRepository {

    private final IndexRateJpaRepository jpaRepository;

    public IndexRateRepositoryAdapter(IndexRateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<IndexRate> findByIndexAndDate(EconomicIndex index, LocalDate referenceDate) {
        return jpaRepository.findByIndexCodeAndReferenceDate(index.name(), referenceDate)
                .map(e -> toDomain(e, false));
    }

    @Override
    public Optional<IndexRate> findLatestByIndex(EconomicIndex index) {
        return jpaRepository.findLatestByIndexCode(index.name())
                .map(e -> toDomain(e, false));
    }

    @Override
    public List<IndexRate> findByIndexAndDateRange(EconomicIndex index, LocalDate from, LocalDate to) {
        return jpaRepository.findByIndexCodeAndReferenceDateBetweenOrderByReferenceDateAsc(
                        index.name(), from, to).stream()
                .map(e -> toDomain(e, false))
                .toList();
    }

    @Override
    public IndexRate save(IndexRate indexRate) {
        IndexRateEntity entity = toEntity(indexRate);
        IndexRateEntity saved = jpaRepository.save(entity);
        return toDomain(saved, indexRate.isProjection());
    }

    private IndexRate toDomain(IndexRateEntity entity, boolean isProjection) {
        return new IndexRate(
                EconomicIndex.valueOf(entity.getIndexCode()),
                entity.getReferenceDate(),
                entity.getRate(),
                isProjection
        );
    }

    private IndexRateEntity toEntity(IndexRate indexRate) {
        IndexRateEntity entity = new IndexRateEntity();
        entity.setIndexCode(indexRate.index().name());
        entity.setReferenceDate(indexRate.referenceDate());
        entity.setRate(indexRate.rate());
        entity.setSource("BCB_SGS");
        return entity;
    }
}
