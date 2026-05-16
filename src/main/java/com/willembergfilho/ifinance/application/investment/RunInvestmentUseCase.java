package com.willembergfilho.ifinance.application.investment;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.index.IndexRate;
import com.willembergfilho.ifinance.domain.index.IndexRateRepository;
import com.willembergfilho.ifinance.domain.investment.*;
import com.willembergfilho.ifinance.domain.math.CalculationException;
import com.willembergfilho.ifinance.domain.math.MonetaryRounding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.UUID;

@Slf4j
@Service
public class RunInvestmentUseCase {

    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
    private static final BigDecimal TRADING_DAYS = new BigDecimal("252");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final InvestmentRepository investmentRepository;
    private final IndexRateRepository indexRateRepository;

    public RunInvestmentUseCase(InvestmentRepository investmentRepository,
                                IndexRateRepository indexRateRepository) {
        this.investmentRepository = investmentRepository;
        this.indexRateRepository = indexRateRepository;
    }

    @Transactional
    public Investment execute(UUID userId, InvestmentParameters parameters) {
        log.info("Running investment: userId={}, type={}, rateBasis={}, principal={}, termDays={}",
                userId, parameters.type(), parameters.rateBasis(),
                parameters.principal(), parameters.termDays());

        MathContext mc = MonetaryRounding.MC;

        BigDecimal indexRateUsed = null;
        BigDecimal grossAnnualRate;

        switch (parameters.rateBasis()) {
            case CDI_PERCENT -> {
                IndexRate cdi = indexRateRepository.findLatestByIndex(EconomicIndex.CDI)
                        .orElseThrow(() -> new CalculationException(
                                "Index rate not available for " + parameters.rateBasis()));
                indexRateUsed = MonetaryRounding.roundRate(cdi.rate());
                BigDecimal dailyDecimal = cdi.rate().divide(HUNDRED, mc);
                BigDecimal cdiAnnual = BigDecimal.valueOf(
                        Math.pow(ONE.add(dailyDecimal, mc).doubleValue(), TRADING_DAYS.doubleValue()) - 1);
                grossAnnualRate = MonetaryRounding.roundRate(cdiAnnual.multiply(parameters.rateValue(), mc));
            }
            case SELIC_PERCENT -> {
                IndexRate selic = indexRateRepository.findLatestByIndex(EconomicIndex.SELIC)
                        .orElseThrow(() -> new CalculationException(
                                "Index rate not available for " + parameters.rateBasis()));
                indexRateUsed = MonetaryRounding.roundRate(selic.rate());
                BigDecimal dailyDecimal = selic.rate().divide(HUNDRED, mc);
                BigDecimal selicAnnual = BigDecimal.valueOf(
                        Math.pow(ONE.add(dailyDecimal, mc).doubleValue(), TRADING_DAYS.doubleValue()) - 1);
                grossAnnualRate = MonetaryRounding.roundRate(selicAnnual.multiply(parameters.rateValue(), mc));
            }
            case IPCA_PLUS -> {
                IndexRate ipca = indexRateRepository.findLatestByIndex(EconomicIndex.IPCA)
                        .orElseThrow(() -> new CalculationException(
                                "Index rate not available for " + parameters.rateBasis()));
                indexRateUsed = MonetaryRounding.roundRate(ipca.rate());
                // BCB returns monthly IPCA in % (e.g. 0.67 = 0.67%), convert to decimal
                BigDecimal ipcaMonthlyDecimal = ipca.rate().divide(HUNDRED, mc);
                BigDecimal ipcaAnnual = BigDecimal.valueOf(
                        Math.pow(ONE.add(ipcaMonthlyDecimal, mc).doubleValue(), 12) - 1);
                grossAnnualRate = MonetaryRounding.roundRate(
                        ONE.add(ipcaAnnual, mc).multiply(ONE.add(parameters.rateValue(), mc), mc).subtract(ONE, mc));
            }
            case PRE_FIXADO -> {
                grossAnnualRate = MonetaryRounding.roundRate(parameters.rateValue());
            }
            default -> throw new CalculationException("Unsupported rate basis: " + parameters.rateBasis());
        }

        double years = parameters.termDays() / DAYS_IN_YEAR.doubleValue();
        double grossFactorDouble = Math.pow(ONE.add(grossAnnualRate, mc).doubleValue(), years);
        BigDecimal grossFactor = BigDecimal.valueOf(grossFactorDouble);
        BigDecimal grossReturn = MonetaryRounding.round(
                parameters.principal().multiply(grossFactor.subtract(ONE, mc), mc));

        boolean taxExempt = parameters.type().isTaxExempt();
        BigDecimal irRate = irRate(parameters.termDays(), taxExempt);
        BigDecimal irAmount = MonetaryRounding.round(grossReturn.multiply(irRate, mc));
        BigDecimal netReturn = MonetaryRounding.round(grossReturn.subtract(irAmount, mc));

        BigDecimal netFactor = ONE.add(netReturn.divide(parameters.principal(), mc), mc);
        double netAnnualRateDouble = Math.pow(netFactor.doubleValue(), DAYS_IN_YEAR.doubleValue() / parameters.termDays()) - 1;
        BigDecimal netAnnualRate = MonetaryRounding.roundRate(BigDecimal.valueOf(netAnnualRateDouble));

        InvestmentResult result = new InvestmentResult(
                grossReturn,
                netReturn,
                irAmount,
                irRate,
                grossAnnualRate,
                netAnnualRate,
                indexRateUsed
        );

        Investment investment = new Investment(UUID.randomUUID(), userId, parameters);
        investment.applyResult(result);
        Investment saved = investmentRepository.save(investment);
        log.info("Investment completed: id={}, grossReturn={}, netReturn={}",
                saved.getId(), result.grossReturn(), result.netReturn());
        return saved;
    }

    private BigDecimal irRate(int termDays, boolean taxExempt) {
        if (taxExempt) return BigDecimal.ZERO;
        if (termDays <= 180) return new BigDecimal("0.225");
        if (termDays <= 360) return new BigDecimal("0.20");
        if (termDays <= 720) return new BigDecimal("0.175");
        return new BigDecimal("0.15");
    }
}
