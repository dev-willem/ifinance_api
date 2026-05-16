package com.willembergfilho.ifinance.application.investment;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.index.IndexRate;
import com.willembergfilho.ifinance.domain.index.IndexRateRepository;
import com.willembergfilho.ifinance.domain.investment.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RunInvestmentUseCaseTest {

    private RunInvestmentUseCase useCase;
    private IndexRateRepository indexRateRepository;
    private InvestmentRepository investmentRepository;

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        indexRateRepository = Mockito.mock(IndexRateRepository.class);
        investmentRepository = Mockito.mock(InvestmentRepository.class);
        when(investmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase = new RunInvestmentUseCase(investmentRepository, indexRateRepository);
    }

    // CDI diário vem do BCB em percentual (ex: 0.0534 = 0.0534%/dia)
    private void stubCdi(String dailyPct) {
        IndexRate rate = new IndexRate(EconomicIndex.CDI, LocalDate.now(), new BigDecimal(dailyPct), false);
        when(indexRateRepository.findLatestByIndex(EconomicIndex.CDI)).thenReturn(Optional.of(rate));
    }

    private void stubIpca(String monthlyPct) {
        IndexRate rate = new IndexRate(EconomicIndex.IPCA, LocalDate.now(), new BigDecimal(monthlyPct), false);
        when(indexRateRepository.findLatestByIndex(EconomicIndex.IPCA)).thenReturn(Optional.of(rate));
    }

    @Test
    void cdb_cdi_irRegressive_365days() {
        stubCdi("0.0534"); // CDI = 0.0534%/dia

        InvestmentParameters params = new InvestmentParameters(
                "CDB 120% CDI", InvestmentType.CDB, RateBasis.CDI_PERCENT,
                new BigDecimal("1.20"), new BigDecimal("50000"), 365, START);

        Investment result = useCase.execute(USER_ID, params);
        InvestmentResult r = result.getResult();

        assertNotNull(r);
        // IR 17.5% (361–720 dias)
        assertEquals(0, new BigDecimal("0.175").compareTo(r.irRate()));
        assertTrue(r.grossReturn().compareTo(BigDecimal.ZERO) > 0);
        // netReturn = grossReturn - irAmount
        assertEquals(r.grossReturn().subtract(r.irAmount()), r.netReturn());
        // netReturn < grossReturn
        assertTrue(r.netReturn().compareTo(r.grossReturn()) < 0);
    }

    @Test
    void lci_taxExempt_noIR() {
        stubCdi("0.0534");

        InvestmentParameters params = new InvestmentParameters(
                "LCI 95% CDI", InvestmentType.LCI, RateBasis.CDI_PERCENT,
                new BigDecimal("0.95"), new BigDecimal("30000"), 180, START);

        Investment result = useCase.execute(USER_ID, params);
        InvestmentResult r = result.getResult();

        assertNotNull(r);
        assertEquals(0, r.irRate().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.irAmount().compareTo(BigDecimal.ZERO));
        assertEquals(r.grossReturn(), r.netReturn());
    }

    @Test
    void preFixado_12pct_annual_360days() {
        InvestmentParameters params = new InvestmentParameters(
                "CDB Pré 12%", InvestmentType.CDB, RateBasis.PRE_FIXADO,
                new BigDecimal("0.12"), new BigDecimal("20000"), 360, START);

        Investment result = useCase.execute(USER_ID, params);
        InvestmentResult r = result.getResult();

        assertNotNull(r);
        assertEquals(0, new BigDecimal("0.12").compareTo(r.grossAnnualRate()));
        // IR 20% (181–360 dias)
        assertEquals(0, new BigDecimal("0.20").compareTo(r.irRate()));
        assertEquals(r.grossReturn().subtract(r.irAmount()), r.netReturn());
    }

    @Test
    void ipcaPlus_combinedRate() {
        stubIpca("0.67"); // IPCA = 0.67%/mês (formato BCB)

        InvestmentParameters params = new InvestmentParameters(
                "Tesouro IPCA+", InvestmentType.TESOURO_IPCA_PLUS, RateBasis.IPCA_PLUS,
                new BigDecimal("0.065"), new BigDecimal("10000"), 720, START);

        Investment result = useCase.execute(USER_ID, params);
        InvestmentResult r = result.getResult();

        assertNotNull(r);
        // grossAnnualRate = (1 + ipcaAnual) × (1 + 6.5%) - 1 > 6.5%
        assertTrue(r.grossAnnualRate().compareTo(new BigDecimal("0.065")) > 0);
        // IR 17.5% (termDays <= 720)
        assertEquals(0, new BigDecimal("0.175").compareTo(r.irRate()));
    }

    @Test
    void irBrackets_180days_is22_5pct() {
        stubCdi("0.0534");

        InvestmentParameters params = new InvestmentParameters(
                "CDB Curto", InvestmentType.CDB, RateBasis.CDI_PERCENT,
                new BigDecimal("1.0"), new BigDecimal("10000"), 180, START);

        Investment result = useCase.execute(USER_ID, params);
        assertEquals(0, new BigDecimal("0.225").compareTo(result.getResult().irRate()));
    }

    @Test
    void irBrackets_181days_is20pct() {
        stubCdi("0.0534");

        InvestmentParameters params = new InvestmentParameters(
                "CDB Medio", InvestmentType.CDB, RateBasis.CDI_PERCENT,
                new BigDecimal("1.0"), new BigDecimal("10000"), 181, START);

        Investment result = useCase.execute(USER_ID, params);
        assertEquals(0, new BigDecimal("0.20").compareTo(result.getResult().irRate()));
    }

    @Test
    void irBrackets_721days_is15pct() {
        stubCdi("0.0534");

        InvestmentParameters params = new InvestmentParameters(
                "CDB Longo", InvestmentType.CDB, RateBasis.CDI_PERCENT,
                new BigDecimal("1.0"), new BigDecimal("10000"), 721, START);

        Investment result = useCase.execute(USER_ID, params);
        assertEquals(0, new BigDecimal("0.15").compareTo(result.getResult().irRate()));
    }

    @Test
    void status_isCalculated() {
        stubCdi("0.0534");

        InvestmentParameters params = new InvestmentParameters(
                "Test", InvestmentType.CDB, RateBasis.CDI_PERCENT,
                new BigDecimal("1.0"), new BigDecimal("1000"), 365, START);

        Investment result = useCase.execute(USER_ID, params);
        assertEquals(InvestmentStatus.CALCULATED, result.getStatus());
    }
}
