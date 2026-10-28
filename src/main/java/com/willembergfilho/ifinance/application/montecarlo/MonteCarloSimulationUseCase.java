package com.willembergfilho.ifinance.application.montecarlo;

import com.willembergfilho.ifinance.domain.montecarlo.MonteCarloParameters;
import com.willembergfilho.ifinance.domain.montecarlo.MonteCarloResult;
import com.willembergfilho.ifinance.domain.montecarlo.MonteCarloSimulator;
import org.springframework.stereotype.Service;

@Service
public class MonteCarloSimulationUseCase {

    private final MonteCarloSimulator simulator = new MonteCarloSimulator();

    public MonteCarloResult execute(MonteCarloParameters params) {
        return simulator.simulate(params);
    }
}
