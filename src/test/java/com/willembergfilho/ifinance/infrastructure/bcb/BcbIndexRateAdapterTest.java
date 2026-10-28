package com.willembergfilho.ifinance.infrastructure.bcb;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.index.IndexRate;
import com.willembergfilho.ifinance.infrastructure.persistence.IndexRateJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.IndexRateEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BcbIndexRateAdapterTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 1);
    private static final LocalDate FEB = LocalDate.of(2026, 2, 1);

    private IndexRateJpaRepository jpa;
    private BcbClient bcb;
    private BcbIndexRateAdapter adapter;

    @BeforeEach
    void setUp() {
        jpa = mock(IndexRateJpaRepository.class);
        bcb = mock(BcbClient.class);
        when(jpa.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jpa.findByIndexCodeAndReferenceDate(anyString(), any())).thenReturn(Optional.empty());
        adapter = new BcbIndexRateAdapter(jpa, bcb);
    }

    private static IndexRateEntity entity(EconomicIndex index, LocalDate date, String rate) {
        IndexRateEntity e = new IndexRateEntity();
        e.setIndexCode(index.name());
        e.setReferenceDate(date);
        e.setRate(new BigDecimal(rate));
        return e;
    }

    @Test
    void findByDateUsesDatabaseWithoutCallingBcb() {
        when(jpa.findByIndexCodeAndReferenceDate("IPCA", JAN)).thenReturn(Optional.of(entity(EconomicIndex.IPCA, JAN, "0.16")));

        Optional<IndexRate> rate = adapter.findByIndexAndDate(EconomicIndex.IPCA, JAN);

        assertThat(rate).contains(new IndexRate(EconomicIndex.IPCA, JAN, new BigDecimal("0.16"), false));
        verifyNoInteractions(bcb);
    }

    @Test
    void findByDateFallsBackToBcbAndPersistsParsedRate() {
        // SGS series 433 = IPCA; decimal comma must be accepted
        when(bcb.fetchSeries(433, JAN, JAN)).thenReturn(List.of(new BcbIndexResponse("01/01/2026", "0,16")));

        Optional<IndexRate> rate = adapter.findByIndexAndDate(EconomicIndex.IPCA, JAN);

        assertThat(rate).contains(new IndexRate(EconomicIndex.IPCA, JAN, new BigDecimal("0.16"), false));
        ArgumentCaptor<IndexRateEntity> saved = ArgumentCaptor.forClass(IndexRateEntity.class);
        verify(jpa).save(saved.capture());
        assertThat(saved.getValue().getIndexCode()).isEqualTo("IPCA");
        assertThat(saved.getValue().getReferenceDate()).isEqualTo(JAN);
        assertThat(saved.getValue().getRate()).isEqualByComparingTo("0.16");
        assertThat(saved.getValue().getSource()).isEqualTo("BCB_SGS");
    }

    @Test
    void bcbUnavailableYieldsEmpty() {
        when(bcb.fetchSeries(anyInt(), any(), any())).thenReturn(List.of());

        assertThat(adapter.findByIndexAndDate(EconomicIndex.CDI, JAN)).isEmpty();
        verify(jpa, never()).save(any());
    }

    @Test
    void malformedBcbRecordsAreSkipped() {
        when(bcb.fetchSeries(eq(433), any(), any())).thenReturn(List.of(
                new BcbIndexResponse("2026-01-01", "0.16"),
                new BcbIndexResponse("01/02/2026", "n/a"),
                new BcbIndexResponse("01/02/2026", "1.31")));

        List<IndexRate> rates = adapter.findByIndexAndDateRange(EconomicIndex.IPCA, JAN, FEB);

        assertThat(rates).containsExactly(new IndexRate(EconomicIndex.IPCA, FEB, new BigDecimal("1.31"), false));
    }

    @Test
    void existingRowIsReusedInsteadOfInsertedTwice() {
        IndexRateEntity existing = entity(EconomicIndex.IPCA, JAN, "0.16");
        when(jpa.findByIndexCodeAndReferenceDate("IPCA", JAN))
                .thenReturn(Optional.empty())            // initial cache lookup
                .thenReturn(Optional.of(existing));      // upsert check
        when(bcb.fetchSeries(433, JAN, JAN)).thenReturn(List.of(new BcbIndexResponse("01/01/2026", "0.16")));

        assertThat(adapter.findByIndexAndDate(EconomicIndex.IPCA, JAN)).isPresent();
        verify(jpa, never()).save(any());
    }

    @Test
    void latestUsesDatabaseFirst() {
        when(jpa.findLatestByIndexCode("SELIC")).thenReturn(Optional.of(entity(EconomicIndex.SELIC, FEB, "0.055131")));

        assertThat(adapter.findLatestByIndex(EconomicIndex.SELIC))
                .contains(new IndexRate(EconomicIndex.SELIC, FEB, new BigDecimal("0.055131"), false));
        verifyNoInteractions(bcb);
    }

    @Test
    void latestFallsBackToLastBcbRecordOfThirteenMonths() {
        when(jpa.findLatestByIndexCode("IPCA")).thenReturn(Optional.empty());
        when(bcb.fetchSeries(eq(433), any(), any())).thenReturn(List.of(
                new BcbIndexResponse("01/01/2026", "0.16"),
                new BcbIndexResponse("01/02/2026", "1.31")));

        assertThat(adapter.findLatestByIndex(EconomicIndex.IPCA))
                .contains(new IndexRate(EconomicIndex.IPCA, FEB, new BigDecimal("1.31"), false));

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        verify(bcb).fetchSeries(eq(433), from.capture(), to.capture());
        assertThat(from.getValue()).isEqualTo(to.getValue().minusMonths(13));
    }

    @Test
    void latestIsEmptyWhenDatabaseAndBcbHaveNothing() {
        when(jpa.findLatestByIndexCode("TR")).thenReturn(Optional.empty());
        when(bcb.fetchSeries(anyInt(), any(), any())).thenReturn(List.of());

        assertThat(adapter.findLatestByIndex(EconomicIndex.TR)).isEmpty();
    }

    @Test
    void rangeUsesDatabaseWhenItHasRows() {
        when(jpa.findByIndexCodeAndReferenceDateBetweenOrderByReferenceDateAsc("IPCA", JAN, FEB))
                .thenReturn(List.of(entity(EconomicIndex.IPCA, JAN, "0.16"), entity(EconomicIndex.IPCA, FEB, "1.31")));

        assertThat(adapter.findByIndexAndDateRange(EconomicIndex.IPCA, JAN, FEB))
                .extracting(IndexRate::referenceDate).containsExactly(JAN, FEB);
        verifyNoInteractions(bcb);
    }

    @Test
    void saveMapsDomainToEntity() {
        IndexRate saved = adapter.save(new IndexRate(EconomicIndex.IGP_M, JAN, new BigDecimal("0.27"), true));

        assertThat(saved).isEqualTo(new IndexRate(EconomicIndex.IGP_M, JAN, new BigDecimal("0.27"), true));
        verify(jpa).save(argThat(e -> e.getIndexCode().equals("IGP_M") && "BCB_SGS".equals(e.getSource())));
    }
}
