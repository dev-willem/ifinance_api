package com.willembergfilho.ifinance.infrastructure.bcb;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.index.IndexRate;
import com.willembergfilho.ifinance.domain.index.IndexRateRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.IndexRateJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.IndexRateEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Primary
@Repository
public class BcbIndexRateAdapter implements IndexRateRepository {

    private static final DateTimeFormatter BCB_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IndexRateJpaRepository jpaRepository;
    private final BcbClient bcbClient;

    public BcbIndexRateAdapter(IndexRateJpaRepository jpaRepository, BcbClient bcbClient) {
        this.jpaRepository = jpaRepository;
        this.bcbClient = bcbClient;
    }

    @Cacheable(value = "indexRates", key = "#index.name() + '_' + #referenceDate")
    @Override
    public Optional<IndexRate> findByIndexAndDate(EconomicIndex index, LocalDate referenceDate) {
        // 1. Try DB first
        Optional<IndexRate> cached = jpaRepository.findByIndexCodeAndReferenceDate(
                        index.name(), referenceDate)
                .map(e -> toRate(e, false));
        if (cached.isPresent()) return cached;

        // 2. Fetch from BCB and persist
        return fetchFromBcb(index, referenceDate, referenceDate).stream().findFirst();
    }

    @Override
    public Optional<IndexRate> findLatestByIndex(EconomicIndex index) {
        Optional<IndexRateEntity> fromDb = jpaRepository.findLatestByIndexCode(index.name());
        if (fromDb.isPresent()) return fromDb.map(e -> toRate(e, false));
        // DB has no data — fetch last 13 months from BCB to find the latest available rate
        LocalDate today = LocalDate.now();
        List<IndexRate> recent = fetchFromBcb(index, today.minusMonths(13), today);
        return recent.isEmpty() ? Optional.empty() : Optional.of(recent.get(recent.size() - 1));
    }

    @Override
    public List<IndexRate> findByIndexAndDateRange(EconomicIndex index, LocalDate from, LocalDate to) {
        List<IndexRateEntity> existing = jpaRepository
                .findByIndexCodeAndReferenceDateBetweenOrderByReferenceDateAsc(index.name(), from, to);
        if (!existing.isEmpty()) {
            return existing.stream().map(e -> toRate(e, false)).toList();
        }
        return fetchFromBcb(index, from, to);
    }

    @Override
    public IndexRate save(IndexRate indexRate) {
        IndexRateEntity entity = new IndexRateEntity();
        entity.setIndexCode(indexRate.index().name());
        entity.setReferenceDate(indexRate.referenceDate());
        entity.setRate(indexRate.rate());
        entity.setSource("BCB_SGS");
        IndexRateEntity saved = jpaRepository.save(entity);
        return toRate(saved, indexRate.isProjection());
    }

    private List<IndexRate> fetchFromBcb(EconomicIndex index, LocalDate from, LocalDate to) {
        log.info("Fetching BCB series: index={}, from={}, to={}", index, from, to);
        List<BcbIndexResponse> responses = bcbClient.fetchSeries(index.getBcbSeriesCode(), from, to);
        log.debug("BCB returned {} records for {}", responses.size(), index);
        return responses.stream()
                .map(r -> persistAndReturn(index, r))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<IndexRate> persistAndReturn(EconomicIndex index, BcbIndexResponse response) {
        try {
            LocalDate date = LocalDate.parse(response.date(), BCB_DATE);
            BigDecimal rate = new BigDecimal(response.value().replace(",", "."));

            IndexRateEntity entity = new IndexRateEntity();
            entity.setIndexCode(index.name());
            entity.setReferenceDate(date);
            entity.setRate(rate);
            entity.setSource("BCB_SGS");

            // Upsert: ignore conflict on (index_code, reference_date)
            Optional<IndexRateEntity> existing = jpaRepository.findByIndexCodeAndReferenceDate(index.name(), date);
            IndexRateEntity saved = existing.orElseGet(() -> jpaRepository.save(entity));
            return Optional.of(toRate(saved, false));
        } catch (Exception e) {
            log.warn("Failed to parse BCB response for {}: {}", index, response);
            return Optional.empty();
        }
    }

    private IndexRate toRate(IndexRateEntity entity, boolean isProjection) {
        return new IndexRate(
                EconomicIndex.valueOf(entity.getIndexCode()),
                entity.getReferenceDate(),
                entity.getRate(),
                isProjection
        );
    }
}
