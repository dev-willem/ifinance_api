package com.willembergfilho.ifinance.application.credit;

import com.willembergfilho.ifinance.domain.credit.CreditCapacityCalculator;
import com.willembergfilho.ifinance.domain.credit.CreditCapacityParameters;
import com.willembergfilho.ifinance.domain.credit.CreditCapacityResult;
import org.springframework.stereotype.Service;

@Service
public class CreditCapacityUseCase {

    private final CreditCapacityCalculator calculator = new CreditCapacityCalculator();

    public CreditCapacityResult execute(CreditCapacityParameters params) {
        return calculator.calculate(params);
    }
}
