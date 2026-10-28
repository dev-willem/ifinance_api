package com.willembergfilho.ifinance.application.credit;

import com.willembergfilho.ifinance.domain.credit.DiscountCalculator;
import com.willembergfilho.ifinance.domain.credit.DiscountParameters;
import com.willembergfilho.ifinance.domain.credit.DiscountResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscountUseCase {

    private final DiscountCalculator calculator = new DiscountCalculator();

    public List<DiscountResult> execute(DiscountParameters params) {
        return calculator.calculate(params);
    }
}
