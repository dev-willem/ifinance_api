package com.willembergfilho.ifinance.application.investment;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.index.IndexRate;
import com.willembergfilho.ifinance.domain.index.IndexRateRepository;
import com.willembergfilho.ifinance.domain.investment.InvestmentParameters;
import com.willembergfilho.ifinance.domain.investment.InvestmentResult;
import com.willembergfilho.ifinance.domain.investment.InvestmentType;
import com.willembergfilho.ifinance.domain.investment.RateBasis;
import com.willembergfilho.ifinance.domain.math.CalculationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectCompareInvestmentsUseCaseTest {

    private IndexRateRepository indexes;
    private DirectCompareInvestmentsUseCase useCase;

    @BeforeEach
    void setUp() {
        indexes = mock(IndexRateRepository.class);
        useCase = new DirectCompareInvestmentsUseCase(indexes);
    }

    private static InvestmentParameters inv(InvestmentType type, RateBasis basis, String rate, int days) {
        return new InvestmentParameters("i", type, basis, new BigDecimal(rate), new BigDecimal("10000"), days,
                LocalDate.of(2026, 1, 1));
    }

    private InvestmentResult single(InvestmentParameters p) {
        return useCase.execute(List.of(p)).getFirst();
    }

    @Test
    void preFixedCdbOneYear() {
        // 10000 * 10% = 1000 gross; 365 days -> 17.5% IR = 175; net 825 -> 8.25% a.a.
        InvestmentResult r = single(inv(InvestmentType.CDB, RateBasis.PRE_FIXADO, "0.10", 365));

        assertThat(r.grossReturn()).isEqualByComparingTo("1000.00");
        assertThat(r.irRate()).isEqualByComparingTo("0.175");
        assertThat(r.irAmount()).isEqualByComparingTo("175.00");
        assertThat(r.netReturn()).isEqualByComparingTo("825.00");
        assertThat(r.netAnnualRate()).isEqualByComparingTo("0.0825");
        assertThat(r.indexRateUsed()).isNull();
    }

    @Test
    void lciIsIncomeTaxExempt() {
        InvestmentResult r = single(inv(InvestmentType.LCI, RateBasis.PRE_FIXADO, "0.10", 365));

        assertThat(r.irRate()).isEqualByComparingTo("0");
        assertThat(r.netReturn()).isEqualByComparingTo("1000.00");
    }

    // Regressive IR table for fixed income - Lei 11.033/2004, art. 1º (in force 2026)
    @ParameterizedTest
    @CsvSource({"180, 0.225", "181, 0.20", "360, 0.20", "361, 0.175", "720, 0.175", "721, 0.15"})
    void regressiveIncomeTaxBrackets(int days, String expected) {
        assertThat(single(inv(InvestmentType.CDB, RateBasis.PRE_FIXADO, "0.10", days)).irRate())
                .isEqualByComparingTo(expected);
    }

    @Test
    void cdiPercentAnnualizesDailyRateOver252Days() {
        // CDI 0.055131% a.d. -> 1.00055131^252 - 1 = 14.90% a.a.; 100% of CDI
        when(indexes.findLatestByIndex(EconomicIndex.CDI)).thenReturn(Optional.of(
                new IndexRate(EconomicIndex.CDI, LocalDate.of(2026, 10, 27), new BigDecimal("0.055131"), false)));

        InvestmentResult r = single(inv(InvestmentType.CDB, RateBasis.CDI_PERCENT, "1.0", 365));

        assertThat(r.indexRateUsed()).isEqualByComparingTo("0.055131");
        assertThat(r.grossAnnualRate().doubleValue()).isCloseTo(0.1489998, within(1e-6));
        assertThat(r.grossReturn().doubleValue()).isCloseTo(1490.00, within(0.01));
    }

    @Test
    void ipcaPlusComposesInflationAndSpread() {
        // IPCA 0.50% a.m. -> 6.1678% a.a.; (1.061678 * 1.06) - 1 = 12.5378% a.a.
        when(indexes.findLatestByIndex(EconomicIndex.IPCA)).thenReturn(Optional.of(
                new IndexRate(EconomicIndex.IPCA, LocalDate.of(2026, 9, 1), new BigDecimal("0.50"), false)));

        InvestmentResult r = single(inv(InvestmentType.TESOURO_IPCA_PLUS, RateBasis.IPCA_PLUS, "0.06", 730));

        assertThat(r.grossAnnualRate().doubleValue()).isCloseTo(0.1253785, within(1e-6));
        assertThat(r.irRate()).isEqualByComparingTo("0.15");
    }

    @Test
    void missingIndexIsCalculationError() {
        when(indexes.findLatestByIndex(EconomicIndex.SELIC)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> single(inv(InvestmentType.TESOURO_SELIC, RateBasis.SELIC_PERCENT, "1.0", 365)))
                .isInstanceOf(CalculationException.class);
    }

    @Test
    void itemCountLimits() {
        InvestmentParameters p = inv(InvestmentType.CDB, RateBasis.PRE_FIXADO, "0.10", 365);

        assertThatThrownBy(() -> useCase.execute(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(Collections.nCopies(6, p))).isInstanceOf(IllegalArgumentException.class);
        assertThat(useCase.execute(Collections.nCopies(5, p))).hasSize(5);
    }
}
