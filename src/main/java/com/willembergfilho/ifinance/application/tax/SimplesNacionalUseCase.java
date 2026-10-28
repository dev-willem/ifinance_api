package com.willembergfilho.ifinance.application.tax;

import com.willembergfilho.ifinance.domain.tax.SimplesNacionalCalculator;
import com.willembergfilho.ifinance.domain.tax.SimplesParameters;
import com.willembergfilho.ifinance.domain.tax.SimplesResult;
import org.springframework.stereotype.Service;

@Service
public class SimplesNacionalUseCase {

    private final SimplesNacionalCalculator calculator = new SimplesNacionalCalculator();

    public SimplesResult execute(SimplesParameters params) {
        return calculator.calculate(params);
    }
}
