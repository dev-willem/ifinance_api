package com.willembergfilho.ifinance.application.risk;

import com.willembergfilho.ifinance.domain.risk.RiskMetricsCalculator;
import com.willembergfilho.ifinance.domain.risk.RiskMetricsParameters;
import com.willembergfilho.ifinance.domain.risk.RiskMetricsResult;
import org.springframework.stereotype.Service;

@Service
public class RiskMetricsUseCase {

    private final RiskMetricsCalculator calculator = new RiskMetricsCalculator();

    public RiskMetricsResult execute(RiskMetricsParameters params) {
        return calculator.calculate(params);
    }
}
