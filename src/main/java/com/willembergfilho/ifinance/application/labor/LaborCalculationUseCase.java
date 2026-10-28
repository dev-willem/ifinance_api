package com.willembergfilho.ifinance.application.labor;

import com.willembergfilho.ifinance.domain.labor.NetSalaryCalculator;
import com.willembergfilho.ifinance.domain.labor.NetSalaryParameters;
import com.willembergfilho.ifinance.domain.labor.NetSalaryResult;
import com.willembergfilho.ifinance.domain.labor.TerminationCalculator;
import com.willembergfilho.ifinance.domain.labor.TerminationParameters;
import com.willembergfilho.ifinance.domain.labor.TerminationResult;
import com.willembergfilho.ifinance.domain.labor.ThirteenthSalaryCalculator;
import com.willembergfilho.ifinance.domain.labor.ThirteenthSalaryParameters;
import com.willembergfilho.ifinance.domain.labor.ThirteenthSalaryResult;
import com.willembergfilho.ifinance.domain.labor.VacationCalculator;
import com.willembergfilho.ifinance.domain.labor.VacationParameters;
import com.willembergfilho.ifinance.domain.labor.VacationResult;
import org.springframework.stereotype.Service;

/** Stateless CLT payroll calculators (net salary, vacation, 13º, termination). */
@Service
public class LaborCalculationUseCase {

    public NetSalaryResult netSalary(NetSalaryParameters params) {
        return NetSalaryCalculator.calculate(params);
    }

    public VacationResult vacation(VacationParameters params) {
        return VacationCalculator.calculate(params);
    }

    public ThirteenthSalaryResult thirteenth(ThirteenthSalaryParameters params) {
        return ThirteenthSalaryCalculator.calculate(params);
    }

    public TerminationResult termination(TerminationParameters params) {
        return TerminationCalculator.calculate(params);
    }
}
