package com.willembergfilho.ifinance.application.index;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.index.IndexRate;
import com.willembergfilho.ifinance.domain.index.IndexRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FetchIndexRateUseCaseTest {

    private IndexRateRepository repo;
    private FetchIndexRateUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = mock(IndexRateRepository.class);
        useCase = new FetchIndexRateUseCase(repo);
    }

    @Test
    void currentDelegatesToLatest() {
        IndexRate latest = new IndexRate(EconomicIndex.IPCA, LocalDate.of(2026, 9, 1), new BigDecimal("0.48"), false);
        when(repo.findLatestByIndex(EconomicIndex.IPCA)).thenReturn(Optional.of(latest));

        assertThat(useCase.fetchCurrent(EconomicIndex.IPCA)).contains(latest);
    }

    @Test
    void pastRangeIsServedFromRepositoryWithoutProjection() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 3, 1);
        List<IndexRate> stored = List.of(new IndexRate(EconomicIndex.IPCA, from, new BigDecimal("0.16"), false));
        when(repo.findByIndexAndDateRange(EconomicIndex.IPCA, from, to)).thenReturn(stored);

        assertThat(useCase.fetchRange(EconomicIndex.IPCA, from, to)).isEqualTo(stored);
        verify(repo, never()).findLatestByIndex(any());
    }

    @Test
    void futureRangeAppendsMonthlyProjectionsWithLatestRate() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusMonths(2);
        LocalDate to = today.plusMonths(3).withDayOfMonth(15);
        IndexRate historical = new IndexRate(EconomicIndex.IPCA, from, new BigDecimal("0.40"), false);
        when(repo.findByIndexAndDateRange(EconomicIndex.IPCA, from, today)).thenReturn(List.of(historical));
        when(repo.findLatestByIndex(EconomicIndex.IPCA))
                .thenReturn(Optional.of(new IndexRate(EconomicIndex.IPCA, today, new BigDecimal("0.48"), false)));

        List<IndexRate> result = useCase.fetchRange(EconomicIndex.IPCA, from, to);

        LocalDate firstOfNext = today.plusMonths(1).withDayOfMonth(1);
        assertThat(result).containsExactly(
                historical,
                new IndexRate(EconomicIndex.IPCA, firstOfNext, new BigDecimal("0.48"), true),
                new IndexRate(EconomicIndex.IPCA, firstOfNext.plusMonths(1), new BigDecimal("0.48"), true),
                new IndexRate(EconomicIndex.IPCA, firstOfNext.plusMonths(2), new BigDecimal("0.48"), true));
    }

    @Test
    void futureRangeWithoutAnyKnownRateReturnsOnlyHistorical() {
        LocalDate today = LocalDate.now();
        when(repo.findByIndexAndDateRange(any(), any(), any())).thenReturn(List.of());
        when(repo.findLatestByIndex(EconomicIndex.CDI)).thenReturn(Optional.empty());

        assertThat(useCase.fetchRange(EconomicIndex.CDI, today.minusDays(5), today.plusMonths(6))).isEmpty();
    }
}
