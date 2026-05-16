package com.willembergfilho.ifinance.domain.index;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IndexRateRepository {

    Optional<IndexRate> findByIndexAndDate(EconomicIndex index, LocalDate referenceDate);

    Optional<IndexRate> findLatestByIndex(EconomicIndex index);

    List<IndexRate> findByIndexAndDateRange(EconomicIndex index, LocalDate from, LocalDate to);

    IndexRate save(IndexRate indexRate);
}
