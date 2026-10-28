package com.willembergfilho.ifinance.application.depreciation;

import com.willembergfilho.ifinance.domain.depreciation.AssetClass;
import com.willembergfilho.ifinance.domain.depreciation.DepreciationCalculator;
import com.willembergfilho.ifinance.domain.depreciation.DepreciationMethod;
import com.willembergfilho.ifinance.domain.depreciation.DepreciationParameters;
import com.willembergfilho.ifinance.domain.depreciation.DepreciationSchedule;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepreciationUseCase {

    private static final List<DepreciationMethod> COMPARED_METHODS = List.of(
            DepreciationMethod.STRAIGHT_LINE,
            DepreciationMethod.SUM_OF_YEARS_DIGITS,
            DepreciationMethod.DECLINING_BALANCE);

    private final DepreciationCalculator calculator = new DepreciationCalculator();

    public DepreciationSchedule execute(DepreciationParameters params) {
        return calculator.calculate(params);
    }

    public List<DepreciationSchedule> compare(DepreciationParameters params) {
        return COMPARED_METHODS.stream()
                .map(m -> calculator.calculate(params.withMethod(m)))
                .toList();
    }

    public List<AssetClass> assetClasses() {
        return List.of(AssetClass.values());
    }
}
