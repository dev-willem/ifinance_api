package com.willembergfilho.ifinance.application.investment;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.index.IndexRate;
import com.willembergfilho.ifinance.domain.index.IndexRateRepository;
import com.willembergfilho.ifinance.domain.investment.InvestmentParameters;
import com.willembergfilho.ifinance.domain.investment.InvestmentResult;
import com.willembergfilho.ifinance.domain.math.CalculationException;
import com.willembergfilho.ifinance.domain.math.MonetaryRounding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

@Slf4j
@Service
public class DirectCompareInvestmentsUseCase {

    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
    private static final BigDecimal TRADING_DAYS = new BigDecimal("252");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final int MAX_ITEMS = 5;

    private final IndexRateRepository indexRateRepository;

    public DirectCompareInvestmentsUseCase(IndexRateRepository indexRateRepository) {
        this.indexRateRepository = indexRateRepository;
    }

    public List<InvestmentResult> execute(List<InvestmentParameters> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one investment must be provided.");
        }
        if (items.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("Cannot compare more than " + MAX_ITEMS + " investments at once.");
        }
        return items.stream().map(this::calculate).toList();
    }

    private InvestmentResult calculate(InvestmentParameters p) {
        MathContext mc = MonetaryRounding.MC;

        BigDecimal indexRateUsed = null;
        BigDecimal grossAnnualRate;

        switch (p.rateBasis()) {
            case CDI_PERCENT -> {
                IndexRate cdi = indexRateRepository.findLatestByIndex(EconomicIndex.CDI)
                        .orElseThrow(() -> new CalculationException("Index rate not available for CDI"));
                indexRateUsed = MonetaryRounding.roundRate(cdi.rate());
                BigDecimal dailyDecimal = cdi.rate().divide(HUNDRED, mc);
                BigDecimal cdiAnnual = BigDecimal.valueOf(
                        Math.pow(ONE.add(dailyDecimal, mc).doubleValue(), TRADING_DAYS.doubleValue()) - 1);
                grossAnnualRate = MonetaryRounding.roundRate(cdiAnnual.multiply(p.rateValue(), mc));
            }
            case SELIC_PERCENT -> {
                IndexRate selic = indexRateRepository.findLatestByIndex(EconomicIndex.SELIC)
                        .orElseThrow(() -> new CalculationException("Index rate not available for SELIC"));
                indexRateUsed = MonetaryRounding.roundRate(selic.rate());
                BigDecimal dailyDecimal = selic.rate().divide(HUNDRED, mc);
                BigDecimal selicAnnual = BigDecimal.valueOf(
                        Math.pow(ONE.add(dailyDecimal, mc).doubleValue(), TRADING_DAYS.doubleValue()) - 1);
                grossAnnualRate = MonetaryRounding.roundRate(selicAnnual.multiply(p.rateValue(), mc));
            }
            case IPCA_PLUS -> {
                IndexRate ipca = indexRateRepository.findLatestByIndex(EconomicIndex.IPCA)
                        .orElseThrow(() -> new CalculationException("Index rate not available for IPCA"));
                indexRateUsed = MonetaryRounding.roundRate(ipca.rate());
                BigDecimal ipcaMonthlyDecimal = ipca.rate().divide(HUNDRED, mc);
                BigDecimal ipcaAnnual = BigDecimal.valueOf(
                        Math.pow(ONE.add(ipcaMonthlyDecimal, mc).doubleValue(), 12) - 1);
                grossAnnualRate = MonetaryRounding.roundRate(
                        ONE.add(ipcaAnnual, mc).multiply(ONE.add(p.rateValue(), mc), mc).subtract(ONE, mc));
            }
            case PRE_FIXADO -> grossAnnualRate = MonetaryRounding.roundRate(p.rateValue());
            default -> throw new CalculationException("Unsupported rate basis: " + p.rateBasis());
        }

        double years = p.termDays() / DAYS_IN_YEAR.doubleValue();
        BigDecimal grossFactor = BigDecimal.valueOf(
                Math.pow(ONE.add(grossAnnualRate, mc).doubleValue(), years));
        BigDecimal grossReturn = MonetaryRounding.round(
                p.principal().multiply(grossFactor.subtract(ONE, mc), mc));

        boolean taxExempt = p.type().isTaxExempt();
        BigDecimal irRate = irRate(p.termDays(), taxExempt);
        BigDecimal irAmount = MonetaryRounding.round(grossReturn.multiply(irRate, mc));
        BigDecimal netReturn = MonetaryRounding.round(grossReturn.subtract(irAmount, mc));

        BigDecimal netFactor = ONE.add(netReturn.divide(p.principal(), mc), mc);
        double netAnnualRateDouble = Math.pow(netFactor.doubleValue(), DAYS_IN_YEAR.doubleValue() / p.termDays()) - 1;
        BigDecimal netAnnualRate = MonetaryRounding.roundRate(BigDecimal.valueOf(netAnnualRateDouble));

        return new InvestmentResult(grossReturn, netReturn, irAmount, irRate, grossAnnualRate, netAnnualRate, indexRateUsed);
    }

    private BigDecimal irRate(int termDays, boolean taxExempt) {
        if (taxExempt) return BigDecimal.ZERO;
        if (termDays <= 180) return new BigDecimal("0.225");
        if (termDays <= 360) return new BigDecimal("0.20");
        if (termDays <= 720) return new BigDecimal("0.175");
        return new BigDecimal("0.15");
    }
}
