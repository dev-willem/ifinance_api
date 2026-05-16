package com.willembergfilho.ifinance.application.analysis;

import com.willembergfilho.ifinance.domain.math.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;

@Slf4j
@Service
public class FinancialAnalysisUseCase {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final NpvCalculator npvCalculator = new NpvCalculator();
    private final IrrCalculator irrCalculator = new IrrCalculator();
    private final PaybackCalculator paybackCalculator = new PaybackCalculator();

    public record AnalysisResult(
            BigDecimal npv,
            boolean npvPositive,
            BigDecimal irrPercent,
            BigDecimal irrDecimal,
            boolean paybackAchieved,
            BigDecimal simplePayback,
            Integer simplePaybackPeriod,
            BigDecimal discountedPayback,
            Integer discountedPaybackPeriod
    ) {}

    public AnalysisResult execute(List<BigDecimal> cashFlows, BigDecimal discountRate) {
        log.info("Financial analysis: cashFlows={}, hasDiscountRate={}", cashFlows.size(), discountRate != null);

        BigDecimal npv = discountRate != null
                ? MonetaryRounding.round(npvCalculator.calculate(cashFlows, discountRate))
                : null;

        BigDecimal irrDecimal = null;
        BigDecimal irrPercent = null;
        try {
            irrDecimal = irrCalculator.calculate(cashFlows);
            irrPercent = MonetaryRounding.round(irrDecimal.multiply(HUNDRED, MC));
        } catch (CalculationException e) {
            log.warn("IRR did not converge for provided cash flows");
        }

        PaybackCalculator.PaybackResult payback = paybackCalculator.calculate(cashFlows, discountRate);

        return new AnalysisResult(
                npv,
                npv != null && npv.compareTo(BigDecimal.ZERO) > 0,
                irrPercent,
                irrDecimal,
                payback.paybackAchieved(),
                payback.simplePayback(),
                payback.simplePaybackPeriod(),
                payback.discountedPayback(),
                payback.discountedPaybackPeriod()
        );
    }
}
