package com.willembergfilho.ifinance.application.index;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.index.IndexRate;
import com.willembergfilho.ifinance.domain.index.IndexRateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class FetchIndexRateUseCase {

    private final IndexRateRepository indexRateRepository;

    public FetchIndexRateUseCase(IndexRateRepository indexRateRepository) {
        this.indexRateRepository = indexRateRepository;
    }

    public Optional<IndexRate> fetchCurrent(EconomicIndex index) {
        return indexRateRepository.findLatestByIndex(index);
    }

    /**
     * Fetches rates for a date range.
     * Dates in the future return a projection using the last known rate.
     */
    public List<IndexRate> fetchRange(EconomicIndex index, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        if (!to.isAfter(today)) {
            return indexRateRepository.findByIndexAndDateRange(index, from, to);
        }

        // Split: historical up to today, project from tomorrow forward
        List<IndexRate> historical = indexRateRepository.findByIndexAndDateRange(index, from, today);
        Optional<IndexRate> latestOpt = indexRateRepository.findLatestByIndex(index);

        if (latestOpt.isEmpty()) return historical;

        IndexRate latest = latestOpt.get();
        LocalDate projectionStart = today.plusMonths(1).withDayOfMonth(1);
        LocalDate projectionEnd = to;

        List<IndexRate> projections = projectionStart.datesUntil(projectionEnd.plusDays(1))
                .filter(d -> d.getDayOfMonth() == 1)
                .map(d -> new IndexRate(index, d, latest.rate(), true))
                .toList();

        return java.util.stream.Stream.concat(historical.stream(), projections.stream()).toList();
    }
}
